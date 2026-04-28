# ADR 001 — Request Context 전파 방식: Explicit Parameter 채택

**상태:** 채택
**날짜:** 2026-04-28
**Phase:** 0 (STEP 3)

## 맥락

멀티 테넌트 SaaS에서 **요청의 테넌트 ID, 분산 추적 ID(traceId)**를 컨트롤러부터 도메인 서비스, 인프라(OutboxWriter 등)까지 어떻게 전파할 것인가.

가이드 문서(`docs/hr-saas-guide.md` STEP 3-3)는 ThreadLocal 기반 `TenantContext` + `TenantFilter` 패턴을 제시. 그러나 사용자 실무 경험에서 **ThreadLocal이 비동기 호출 시 깨지는 사례**가 보고되어 재평가 필요.

> **명명 결정**: `correlationId` 대신 **`traceId`** 채택. W3C Trace Context 및 OpenTelemetry 표준이 `traceId`를 사용하며, Spring Cloud Sleuth → Micrometer Tracing 진화 흐름과 일관됨. 향후 OTel 통합 시 매핑 비용 0.

## 결정

**ThreadLocal 폐기. Explicit Parameter 패턴 채택.**

`RequestContext` record 하나로 테넌트 ID와 trace ID를 묶고, 모든 도메인 서비스 메서드의 첫 번째 파라미터로 명시적 전달.

```java
public record RequestContext(UUID tenantId, UUID traceId) {
    public static RequestContext of(UUID tenantId) {
        return new RequestContext(tenantId, Ids.generate());
    }
}

// 사용 예
class EmployeeService {
    UUID create(RequestContext ctx, CreateEmployeeCommand cmd) { ... }
}
```

Inbound(Controller)에서 `@RequestHeader("X-Tenant-Id")`로 받아 `RequestContext` 구성 후 서비스 호출.

## 근거

1. **비동기 안전성** — `@Async`, `CompletableFuture`, Reactive(WebFlux) 전환 시 ThreadLocal은 자동 전파되지 않아 silent failure 위험. Explicit 전달은 어떤 실행 모델에서도 안전.

2. **Virtual Thread + ScopedValue 미래 대비** — Java 21+ Project Loom이 `ScopedValue`(Java 22+ preview)를 도입한 이유 자체가 ThreadLocal 한계. Explicit 패턴은 자연스럽게 ScopedValue로 전환 가능.

3. **명시성** — 메서드 시그니처에 테넌트 의존성이 드러남. 암묵적 ThreadLocal보다 코드 리뷰·테스트 용이.

4. **테스트 단순화** — `RequestContext.of(testTenantId)` 인자 전달이면 끝. ThreadLocal set/clear 라이프사이클 관리 불필요.

5. **모듈러 모놀리스 정체성과 부합** — 모듈 간 통신 시 의존성을 명시적으로 드러내는 원칙과 일관.

## 대안 검토

### 대안 1: ThreadLocal (가이드 원안)
- 장점: 메서드 시그니처 깔끔, 파라미터 오염 없음
- 단점: 비동기 깨짐(실무 사례), Reactive 비호환, 누수 위험, 암묵적 상태로 테스트 복잡
- **기각 이유:** 사용자 실무에서 검증된 함정. 동일 함정 의도적 회피.

### 대안 2: 하이브리드 (ThreadLocal + 명시 파라미터 둘 다)
- 장점: 두 패턴 모두 사용 가능
- 단점: 복잡도 2배, "어느 게 진실?" 혼란, 동기화 부담
- **기각 이유:** YAGNI. 단일 패턴이 단순하고 안전.

### 대안 3: AOP로 자동 주입
- 장점: 보일러플레이트 감소
- 단점: 마법의 양 증가, 디버깅 난이도 ↑, 컴파일 시점 검증 X
- **기각 이유:** 명시성과 트레이드오프 안 맞음.

## 결과

### 즉시 영향
- `TenantContext`, `TenantFilter`, `CorrelationContext` **만들지 않음** (가이드 원안에서 이탈)
- `RequestContext` record 1개로 통합 (`shared-kernel/.../context/RequestContext.java`)
- `OutboxWriter` 시그니처가 `RequestContext`를 첫 인자로 받음
- Controller에서 `@RequestHeader("X-Tenant-Id") UUID tenantId`로 헤더 직접 수신

### 장기 영향
- 모든 도메인 서비스 메서드 시그니처에 `RequestContext` 파라미터 (파라미터 +1)
- Reactive/Virtual Thread/ScopedValue 전환 시 코드 변경 최소
- 가이드 문서(STEP 3-3, 3-4) 갱신 필요

### 수용한 비용
- 메서드마다 `ctx` 파라미터 추가 (sigture verbosity)
- 레이어 통과 시 수동 전달

## 참조

- `docs/engineering-log.md` — 의사결정 과정과 함정 사례 (서사)
- 사용자 실무 사례: ThreadLocal 기반 멀티테넌시가 비동기에서 깨진 경험
- [Java Project Loom: ScopedValue](https://openjdk.org/jeps/429) — JEP 429
- 가이드 문서 `docs/hr-saas-guide.md` STEP 3-3 (참고용 원안)
