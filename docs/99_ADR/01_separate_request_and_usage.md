# ADR 01: 연차 신청(Request)과 차감 상세(Usage)의 엔티티 분리

* **Status:** Accepted
* **Date:** 2025-12-10
* **Author:** AI Agent (Reviewer) & User (Driver)

---

## 1. Context (배경 및 문제점)

연차 사용 로직을 설계하던 중, **"선입선출(FIFO) 차감 정책"** 으로 인해 다음과 같은 문제가 식별되었다.

1.  **1:N 차감 발생:** 사용자가 '1일'을 신청하더라도, 실제로는 잔여 연차 상황에 따라 `Grant A(0.2일)`와 `Grant B(0.8일)` 두 개에서 나뉘어 차감될 수 있다.
2.  **데이터 추적의 어려움:** 만약 `LeaveUsage`라는 하나의 엔티티에 `deductedGrantIds=[A, B]`와 같이 단순 리스트로만 저장한다면, **"각 Grant에서 정확히 몇 점을 차감했는지"** 알 수 없다.
3.  **취소(Rollback) 불가능:** 신청을 취소할 때, A에게 0.2를 돌려주고 B에게 0.8을 돌려줘야 하는데, 위 정보가 없으면 정확한 환불(Refund) 로직을 구현할 수 없다.

## 2. Options Considered (고려한 대안들)

*   **Option A: Value Object 사용**
    *   `LeaveUsage` 내부에 `List<DeductionRecord>` 같은 VO 컬렉션을 포함하여 JSON 등으로 저장.
    *   *단점:* JPQL 등으로 "특정 Grant를 사용한 내역"을 역으로 조회하거나 통계 낼 때 쿼리가 복잡해짐.

*   **Option B: 단일 엔티티 분할 저장**
    *   사용자가 1건 신청 시, 시스템이 내부적으로 `LeaveUsage` 엔티티 2개를 생성.
    *   *단점:* "신청(Request)"이라는 사용자의 의도(Intent)와 "차감(Usage)"이라는 결과(Result)가 혼재되어, UI 표현이나 상태 관리(`PENDING`, `APPROVED`)가 모호해짐.

*   **Option C: 신청(Request)과 상세(Usage)의 엔티티 분리 (Selected)**
    *   `LeaveRequest` (부모): 사용자의 신청 행위 관리.
    *   `LeaveUsage` (자식): 실제 차감된 영수증/장부 내역 관리.

## 3. Decision (결정 사항)

우리는 **Option C (엔티티 분리)** 방식을 채택한다.

*   **`LeaveRequest` Entity:**
    *   사용자의 신청 내역(신청일, 휴가일, 총 사용량, 상태)을 관리한다.
    *   Aggregate Root인 `LeaveAccount`가 `List<LeaveRequest>`를 관리한다.
*   **`LeaveUsage` Entity:**
    *   `LeaveRequest`의 하위 엔티티로 존재한다 (1:N 관계).
    *   `grantId`와 `amount`를 명시하여, "어떤 Grant에서 얼마를 썼는지" 정확히 기록한다.

## 4. Consequences (결과 및 트레이드오프)

### Positive (장점)
*   **정확한 데이터 정합성:** 취소 시 `Usage` 리스트를 순회하며 각 `Grant`에 정확한 수치를 환불해줄 수 있다.
*   **책임의 분리 (SRP):**
    *   `Request`: 상태 관리(승인/반려/취소) 및 사용자 인터페이스 대응.
    *   `Usage`: 재무적 차감 처리 및 회계 기록.
*   **유연성:** 추후 "연차 사용 내역 통계" 등을 뽑을 때 `Usage` 테이블만 조회하면 되므로 편리하다.

### Negative (단점)
*   **구현 복잡도 증가:** 엔티티가 하나 늘어났으므로 테이블 조인(Join) 비용이 발생하고, 저장 시 로직이 약간 길어진다.
*   **스토리지 증가:** 데이터 로우(Row) 수가 늘어난다. (하지만 연차 데이터 특성상 대용량이라 해도 감당 가능한 수준이다.)

---

## 5. Related Documents
* `docs/30_DDD_Model/01_aggregate_definition.md`
