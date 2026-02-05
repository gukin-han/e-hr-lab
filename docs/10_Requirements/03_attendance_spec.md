# 출퇴근 서비스 설계 명세서

> **목표:** 대규모 트래픽 처리 경험을 위한 출퇴근 서비스 구축
> **전략:** 가장 단순한 구조에서 시작 → 병목 발견 → 점진적 확장

---

## 1. 설계 원칙

| 항목 | 결정 | 근거 |
|------|------|------|
| 멀티테넌트 | `tenantId` 필수 | SaaS 구조 대비 |
| 시간 기록 | 서버 시간 (UTC) 저장 | 클라이언트 조작 방지, 원장 일관성 |
| 시간 처리 | 집계/상태 계산 시 테넌트 타임존 적용 | 지역별 날짜 경계 처리 |
| 출근 검증 | 당일 마지막 기록이 출근이 아닐 때만 출근 가능 | 중복 출근 방지 |
| 집계 방식 | 기록 시점에 일집계 즉시 갱신 | 실시간 상태 반영 |
| 트랜잭션 | 기록 + 집계를 단일 트랜잭션 | 정합성 우선 (Phase 1) |

### 1.1 시간 처리 전략

| 필드 | 성격 | 설명 |
|------|------|------|
| `recordedAt` | 기술적 타임스탬프 | UTC 저장, 원장 불변, 감사 추적용 |
| `workDate` | **비즈니스 개념 (근무일)** | 정책에 따라 결정, 집계/상태의 기준 |

```
recordedAt (UTC)  ──→  workDate 결정 로직  ──→  workDate (LocalDate)
                       (비즈니스 규칙)
```

**Phase 1:** 테넌트 타임존 기반 단순 변환
**확장:** 근무 스케줄, shift boundary 등 비즈니스 규칙 적용

**실무 확장 고려사항 (미구현):**
- 야간 근무자: 새벽 2시 퇴근 → 전날 근무로 처리
- 근무 스케줄 기반 날짜 결정 (shift boundary)
- `workDate`는 Value Object 또는 Domain Service로 캡슐화 가능
- 이 복잡도는 대용량 트래픽 실험 목적상 Phase 1에서 제외

---

## 2. 데이터 계층 구조

### Phase 1: 단순 구조로 시작

```
┌─────────────────────────────────────────────────────────────┐
│  DailyAttendance (상태 + 일집계 통합)                        │
│  - 당일 근태 상태 (WORKING/LEFT)                            │
│  - 일별 집계 (총 근무시간 등)                                │
│  - 기간 조회 시 쿼리로 집계                                  │
└─────────────────────────────────────────────────────────────┘
                          ▲ 출퇴근 시 즉시 갱신
┌─────────────────────────────────────────────────────────────┐
│  AttendanceRecord (출퇴근 원장)                              │
│  - 개별 이벤트 로그 (불변, Append-only)                      │
│  - 감사 추적용 원본 데이터                                   │
└─────────────────────────────────────────────────────────────┘
```

### 확장 계획: 필요 시 분리

```
┌─────────────────────────────────────────────────────────────┐
│  PeriodSummary (기간 집계) ← 급여 확정 시 스냅샷 저장         │
└─────────────────────────────────────────────────────────────┘
                          ▲ 급여 확정 시점에 생성
┌─────────────────────────────────────────────────────────────┐
│  DailySummary (일집계) ← DailyAttendance에서 분리            │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│  AttendanceStatus (상태) ← DailyAttendance에서 분리          │
│  - 실시간 상태 조회 최적화 필요 시                            │
└─────────────────────────────────────────────────────────────┘
                          ▲ 출퇴근 시 즉시 갱신
┌─────────────────────────────────────────────────────────────┐
│  AttendanceRecord (출퇴근 원장)                              │
└─────────────────────────────────────────────────────────────┘
```

**분리 트리거:**
- 상태 조회 성능 병목 발생 시 → AttendanceStatus 분리
- 일집계 로직 복잡화 시 → DailySummary 분리
- 급여 연동 필요 시 → PeriodSummary 추가

---

## 3. 엔티티 설계

### 3.1 AttendanceRecord (출퇴근 원장)

개별 출근/퇴근 이벤트 로그. **불변(Immutable), Append-only**.

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `tenantId` | Long | 테넌트 식별자 |
| `employeeId` | Long | 직원 식별자 |
| `recordType` | Enum | `CHECK_IN`, `CHECK_OUT` |
| `recordedAt` | Instant | 서버 기준 UTC 시간 |
| `workDate` | LocalDate | 근무일 (조회 편의) |

**인덱스:**
- `(tenantId, employeeId, workDate)` - 일별 기록 조회

### 3.2 DailyAttendance (상태 + 일집계 통합)

해당 날짜의 근태 **상태** 및 **일별 집계**를 통합 관리. **출퇴근 시 실시간 갱신**.

> Phase 1에서는 상태와 일집계를 하나의 엔티티로 관리.
> 성능 병목 발생 시 AttendanceStatus / DailySummary로 분리 가능.

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `tenantId` | Long | 테넌트 식별자 |
| `employeeId` | Long | 직원 식별자 |
| `workDate` | LocalDate | 근무일 |
| `status` | Enum | `NOT_STARTED`, `WORKING`, `LEFT` |
| `firstCheckIn` | Instant | 최초 출근 시간 |
| `lastCheckOut` | Instant | 최종 퇴근 시간 |
| `totalWorkMinutes` | Integer | 총 근무 시간 (분) |

**인덱스:**
- `(tenantId, employeeId, workDate)` - UK, 일별 상태 조회

### 3.3 PeriodSummary (확장 시 추가)

> **Phase 1에서는 구현하지 않음.** 기간 조회는 DailyAttendance 쿼리로 처리.
> 급여 확정 스냅샷이 필요할 때 도입.

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `tenantId` | Long | 테넌트 식별자 |
| `employeeId` | Long | 직원 식별자 |
| `periodStart` | LocalDate | 집계 시작일 |
| `periodEnd` | LocalDate | 집계 종료일 |
| `totalWorkDays` | Integer | 총 근무일수 |
| `totalWorkMinutes` | Integer | 총 근무 시간 (분) |
| `lateCount` | Integer | 지각 횟수 |
| `earlyLeaveCount` | Integer | 조퇴 횟수 |
| `absentCount` | Integer | 결근 횟수 |
| `calculatedAt` | Instant | 집계 시점 |

**도입 트리거:**
- 급여 시스템 연동 필요 시
- 기간 조회 성능 병목 발생 시

---

### 3.4 Phase 1 기간 조회 전략

Phase 1에서는 PeriodSummary 없이 **DailyAttendance 쿼리**로 처리:

```sql
SELECT
  COUNT(*) as totalWorkDays,
  SUM(totalWorkMinutes) as totalWorkMinutes
FROM DailyAttendance
WHERE tenantId = ? AND employeeId = ?
  AND workDate BETWEEN ? AND ?
```

| 조회 주체 | 시나리오 | 처리 방식 |
|----------|---------|----------|
| 직원 | "이번 달 내 근무시간" | DailyAttendance 쿼리 |
| 관리자 | "팀원 이번 주 현황" | DailyAttendance 쿼리 |
| 급여팀 | "1월 급여 정산" | DailyAttendance 쿼리 → 추후 PeriodSummary |

---

## 4. API 설계

### 4.1 출퇴근 기록 API

```
POST /api/v1/attendances/check
```

**Request:**
```json
{
  "tenantId": 1,
  "employeeId": 100,
  "type": "CHECK_IN"
}
```

**Response:**
```json
{
  "recordId": 1,
  "recordedAt": "2024-01-15T00:30:00Z",
  "dailyStatus": "WORKING"
}
```

**비즈니스 로직:**
1. 서버 시간(UTC)으로 `recordedAt` 생성
2. `DailyAttendance` 조회 (없으면 생성)
3. 출근 검증: `status`가 `WORKING`이면 출근 불가
4. `AttendanceRecord` 저장
5. `DailyAttendance` 상태 갱신
6. 단일 트랜잭션으로 처리

---

## 5. 확장 로드맵

### Phase 1: 단순 동기 구조 (현재)
```
Client → Controller → Service → DB (단일 트랜잭션)
```
- **엔티티:** AttendanceRecord + DailyAttendance (2개)
- **아키텍처:** Transaction Script
- **목표:** 기본 기능 구현 + 병목 지점 파악
- **측정:** k6 부하 테스트

### Phase 2: 병목 대응 (예정)

| 병목 | 해결 전략 | 학습 포인트 |
|------|----------|------------|
| DB 커넥션 고갈 | HikariCP 튜닝 | 풀 사이즈 vs 스레드 수 |
| DB 쓰기 지연 | 비동기 큐 도입 | 메시지 큐, 이벤트 드리븐 |
| 동시성 충돌 | 낙관적/비관적 락 | 동시성 제어 패턴 |
| 스케일 한계 | CQRS, 이벤트 소싱 | 쓰기/읽기 분리 |

---

## 6. 미결 사항 (TODO)

### Phase 1 구현 전 결정 필요
- [ ] 목표 성능 지표 정의: TPS, p99 latency 등

### Phase 1 이후 (확장 시 결정)
- [ ] `workDate` 결정 로직 고도화: 근무 스케줄, shift boundary 적용
- [ ] 총 근무 시간 계산: 출퇴근 쌍 기준? 외출/복귀 고려?
- [ ] 지각/조퇴/결근 판정 기준: 테넌트별 설정? (예: 9시 이후 출근 = 지각)
- [ ] 기간 집계 재계산: 원장 데이터 수정 시 어디까지 재집계?
- [ ] PeriodSummary 도입 시점 및 스키마

### 결정 완료
- [x] 데이터 계층: Phase 1은 2단계 (Record + DailyAttendance), 필요 시 확장
- [x] 기간 조회: Phase 1은 DailyAttendance 쿼리로 처리
- [x] 시간 처리: `recordedAt`은 UTC 저장, `workDate`는 비즈니스 개념으로 분리
- [x] Phase 1 workDate: 테넌트 타임존 기반 단순 변환 (확장 가능 구조)
