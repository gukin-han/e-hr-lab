# Phase 0 / STEP 3-3 — OutboxWriter

> Outbox 패턴의 핵심. 모듈러 모놀리스에서 MSA로 진화 가능성을 결정하는 단 하나의 컴포넌트.

## 왜 이 컴포넌트가 필요한가

**Outbox 패턴 = 도메인 변경과 이벤트 기록을 같은 트랜잭션에 묶는 기법.**

가이드 문서 3-4에서 강조: **"이벤트 브로커는 나중에, Outbox는 Day 1부터."**

- 도메인 변경 + Outbox INSERT를 **같은 트랜잭션**에 → 이벤트 손실 0
- 나중에 Publisher만 붙이면 즉시 이벤트 발행 (Kafka, RabbitMQ 등)
- 도입 비용 거의 0 (테이블 1개 + INSERT 1번)

**없으면:** "이벤트 기반으로 전환하려면 모든 Service 메서드 수정해야 함" 지옥.

## 설계 — 두 컴포넌트 분리

### A. `DomainEvent` 인터페이스 (`shared.event`)

모든 도메인 이벤트가 구현할 계약. Outbox에 저장될 때 메타데이터를 자동으로 끌어내기 위함.

```java
public interface DomainEvent {
    String aggregateType();   // "Employee", "LeaveRequest" 등
    String aggregateId();     // UUID.toString() 또는 비즈니스 ID
    String eventType();       // "EmployeeCreated", "LeaveRequested" 등
}
```

**왜 String?** JSON 직렬화·로그·검색 친화적. 이벤트 타입은 도메인이 자유롭게 명명.

### B. `OutboxWriter` 클래스 (`shared.outbox`)

```java
@Component
public class OutboxWriter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(RequestContext ctx, DomainEvent event) { ... }
}
```

## 핵심 판단 포인트 5가지

### 1. `Propagation.MANDATORY` — 가장 중요한 결정

```java
@Transactional(propagation = Propagation.MANDATORY)
```

| 옵션 | 동작 | 평가 |
|---|---|---|
| `REQUIRED` (기본) | 트랜잭션 없으면 **새로 시작** | ❌ Outbox 패턴 깨짐 (도메인 tx와 분리) |
| `MANDATORY` | 트랜잭션 없으면 **예외** | ✅ "반드시 호출자 tx 안에서만" 강제 |
| `REQUIRES_NEW` | 항상 **새 tx** 시작 | ❌ 명시적 anti-pattern |

**MANDATORY 선택 이유:** Outbox INSERT가 도메인 변경과 다른 tx에 있으면 → 도메인은 커밋, 이벤트는 롤백 (또는 반대) → **데이터 일관성 깨짐.** 반드시 같은 tx여야 함. MANDATORY로 강제.

### 2. JdbcTemplate vs JPA

JPA 안 씀. 이유:
- Outbox는 **인프라**지 도메인 X — Entity 모델링 가치 없음
- JPA의 dirty checking, lazy loading 같은 마법 불필요
- INSERT 한 줄이면 끝 — 가벼움
- **JpaRepository로 만들면 다른 모듈에서 import 가능해져 경계 무너짐 위험**

JdbcTemplate으로 직접 SQL.

### 3. MySQL JSON 컬럼 — `CAST(? AS JSON)` 명시

```sql
INSERT INTO outbox_events (..., payload, ...)
VALUES (..., CAST(? AS JSON), ...)
```

MySQL JSON 컬럼에 String 그대로 바인딩하면 타입 미스매치. **`CAST(? AS JSON)`로 명시 변환** 필수.

### 4. `ObjectMapper` + `JavaTimeModule`

이벤트 payload에 `LocalDate`, `LocalDateTime` 자주 등장 (입사일, 신청일 등). Jackson 기본 `ObjectMapper`는 이걸 직렬화 못 함.

**해결:** App 진입점(`EhrLabApplication`)에 ObjectMapper Bean에 `JavaTimeModule` 등록 + `application.yml`에 `jackson.serialization.write-dates-as-timestamps: false`.

→ OutboxWriter 자체엔 영향 없음, 주입받는 ObjectMapper에 모듈 등록되면 끝.

### 5. `JsonProcessingException` 처리

`mapper.writeValueAsString(event)`은 `JsonProcessingException` (checked) 던짐. 호출 측이 try-catch로 덮으면 사용 코드가 더러워짐.

**관행:** `IllegalStateException`으로 wrap (직렬화 못 하는 이벤트 = 프로그래머 실수, runtime 예외 적합).

```java
try {
    String payload = mapper.writeValueAsString(event);
    // ...
} catch (JsonProcessingException e) {
    throw new IllegalStateException("Failed to serialize event: " + event.eventType(), e);
}
```

## 요구사항

### 파일 1: `shared-kernel/src/main/java/dev/gukin/ehrlab/shared/event/DomainEvent.java`

- 인터페이스
- 3개 추상 메서드: `aggregateType()`, `aggregateId()`, `eventType()` 모두 `String` 반환

### 파일 2: `shared-kernel/src/main/java/dev/gukin/ehrlab/shared/outbox/OutboxWriter.java`

- `@Component`
- 의존성 주입: `JdbcTemplate`, `ObjectMapper` (생성자 주입 또는 Lombok `@RequiredArgsConstructor`)
- `@Transactional(propagation = Propagation.MANDATORY)`
- 메서드 시그니처:
  ```java
  public void append(RequestContext ctx, DomainEvent event)
  ```
- 동작:
  1. 이벤트 ID 생성 (`Ids.generate()`)
  2. payload JSON 직렬화 (`mapper.writeValueAsString(event)`)
  3. INSERT 실행
  4. UUID들은 `Ids.toBytes()`로 변환해서 바인딩
  5. `JsonProcessingException`은 `IllegalStateException`으로 wrap

### SQL 템플릿

```java
"""
INSERT INTO ehrlab_shared.outbox_events
  (id, tenant_id, aggregate_type, aggregate_id, event_type, payload, trace_id)
VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), ?)
"""
```

## 자주 나올 Q&A

**Q1: "outbox_events 테이블이 아직 없는데 컴파일 되나?"**
A: 컴파일은 됨. `JdbcTemplate.update`은 SQL 문자열만 받음. **실제 INSERT는 STEP 4에서 Flyway로 테이블 만든 뒤 가능.** STEP 7 통합 테스트에서 검증.

**Q2: "MANDATORY인데 호출자가 tx 없으면 어쩌나?"**
A: `IllegalTransactionStateException` 발생. **"왜 tx 없이 도메인 변경 호출?"**이라는 명백한 코드 버그 — 즉시 발견됨. 좋은 강제력.

**Q3: "Outbox에서 발행은 누가? Publisher?"**
A: STEP 외 — Phase 3 또는 별도 batch job. 우리는 지금 **WRITE만** 구현. Publisher (relay)는 추후.

**Q4: "이벤트 발행 순서 보장되나?"**
A: 같은 tx 내 INSERT는 commit 시점 순서 그대로. Publisher가 `occurred_at` 순으로 폴링하면 순서 유지. **단, 다른 트랜잭션 간 순서는 보장 X** (필요 시 별도 설계).

**Q5: "테스트는?"**
A: Unit으로는 mock JdbcTemplate. 가치 낮음. **Testcontainers + 실제 INSERT 검증**이 STEP 7에서 의미 있음.

## 참조

- 가이드 문서 `docs/hr-saas-guide.md` STEP 3-5 (원안 코드 예시)
  - 단 `correlation_id` → 우리는 `trace_id` 사용 (이름 정정, ADR 001)
  - `CorrelationContext.current()` 호출 → `ctx.traceId()`로 변경 (ThreadLocal 폐기, ADR 001)
- ADR 001 — `docs/adr/001_request_context_propagation.md`
- engineering log "ThreadLocal 폐기" 엔트리

## 변경 이력

| 날짜 | 내용 |
|---|---|
| 2026-04-28 | 초안 — STEP 3-3 멘토링 가이드 |
