# ADR-02: DailyAttendance 중심 상태 관리

## 상태
**승인됨 (Accepted)**

## 맥락
출퇴근 서비스에서 "출근 가능 여부"를 판단할 때 두 가지 접근법이 있다:

1. **원장 기반**: AttendanceRecord에서 당일 마지막 기록 조회
2. **상태 기반**: DailyAttendance의 status 필드로 판단

스펙 문서에는 "당일 마지막 기록이 출근이 아닐 때만 출근 가능"으로 기술되어 있어 원장 기반을 암시했으나, 구현 과정에서 상태 기반이 더 적합하다고 판단했다.

## 결정
**DailyAttendance를 상태 판단의 중심점으로 사용한다.**

```java
// 서비스: 오케스트레이션만
DailyAttendance daily = findOrCreate(...);
daily.checkIn(now);  // 검증은 엔티티 내부에서

// 엔티티: 비즈니스 규칙 보유
public void checkIn(Instant time) {
    if (this.status == WORKING) {
        throw new IllegalStateException("이미 출근 상태");
    }
    this.status = WORKING;
}
```

## 이유

### 1. 단일 조회로 판단 가능
- 원장 기반: `SELECT * FROM record WHERE ... ORDER BY recordedAt DESC LIMIT 1`
- 상태 기반: `SELECT * FROM daily WHERE tenantId=? AND employeeId=? AND workDate=?`
- 상태 기반이 더 직관적이고 인덱스 활용 용이

### 2. 비즈니스 규칙 캡슐화
- Transaction Script 패턴이지만 검증 로직은 엔티티에 위치
- 서비스는 흐름 제어만 담당
- "Rich Domain Model in Transaction Script" 형태

### 3. DDD 전환 용이
- DailyAttendance가 사실상 Aggregate Root 역할
- 향후 DDD로 전환 시 자연스러운 구조
- AttendanceRecord는 내부 Entity 또는 Domain Event로 전환 가능

## 결과

### 긍정적
- 명확한 책임 분리 (서비스: 흐름, 엔티티: 규칙)
- 상태 변경과 검증이 같은 위치에서 처리
- 테스트 용이 (엔티티 단위 테스트 가능)

### 주의점
- DailyAttendance 상태가 잘못되면 원장과 불일치 가능
- 필요 시 원장 기반 복구 로직 구현 필요
- 상태와 원장의 정합성 모니터링 고려

## 관련 문서
- [03_attendance_spec.md](../10_Requirements/03_attendance_spec.md)
- [01_separate_request_and_usage.md](./01_separate_request_and_usage.md) (연차 도메인 유사 패턴)
