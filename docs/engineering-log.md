# Engineering Log

의사결정 과정과 인사이트를 시간순으로 기록. Phase 종료 시 블로그 초안의 재료가 됨.

각 엔트리 형식:
- **맥락**: 어떤 결정/문제에 직면했는가
- **논의**: 고려한 옵션들과 trade-off
- **결론**: 최종 선택과 이유
- **블로그 각도**: 글감 후보
- **인사이트/함정**: 놀랐던 것, 배운 것
- **코드 포인터**: 관련 파일/커밋

---

## 2026-04-20 — UUID 저장 전략 (CHAR(36) vs BINARY(16)), Converter vs Hibernate native

### 맥락
Phase 0 STEP 3에서 `UuidToBinaryConverter` 구현 중 의문 발생. 가이드 문서(SB 3.3 기준)대로 따라가다가 "정말 필요한가?"라는 근본 질문 제기됨.

### 논의

**1차 의문: Converter 클래스 자체가 필요한가?**
- 가이드는 `@Converter(autoApply=true)` 방식 제시
- Hibernate 7에선 `@JdbcTypeCode(SqlTypes.BINARY)` 한 줄로 동일 효과
- 가이드가 구식이었음을 확인

**2차 의문: BINARY(16)이 실무적으로 의미 있나?**
- 한국 업계 실태 체감:
  - `CHAR(36)` / `VARCHAR(36)` — 대부분의 팀 (~60-70%)
  - `BIGINT AUTO_INCREMENT` — 전통 대기업 레거시 (~20-25%)
  - `BINARY(16)` — 소수 빅테크/성능팀 (~5-10%)
- `BINARY(16)`의 운영 고통: `BIN_TO_UUID(id)`, `UUID_TO_BIN()` 상시 입력. 새벽 장애 대응 시 특히 불편.
- 이론적 이득(2.25x 저장, 빠른 비교, 인덱스 크기 감소)은 **수천만 행 이상** 규모에서 실감.

**3차 의문: 벤치마크 없이 선택하는 게 말이 되나?**
- "직접 해봐야 진짜 감각이 생긴다"
- 최소 한 번은 손으로 만져보지 않으면 나중에도 모호함
- **해법: 지금은 B로 가되, Phase 4~5에 별도 벤치마크 진행 + 블로그화**

### 결론

1. **`UuidToBinaryConverter` 클래스 작성하지 않음.** 대신 `Ids.toBytes()` / `fromBytes()` static 메서드만 추가.
2. **JPA 엔티티는 `@JdbcTypeCode(SqlTypes.BINARY)` 사용** (Hibernate native).
3. **OutboxWriter 등 JdbcTemplate 쓰는 곳은 `Ids.toBytes()` 호출.**
4. **Phase 4 또는 5에 벤치마크 세션 예약**: CHAR(36) vs BINARY(16) 실측 비교.

### 블로그 각도

1. **"MySQL UUID 저장 — CHAR(36) vs BINARY(16) 실측 비교"** 👑
   - Phase 4/5에서 벤치마크 후 작성
   - 100만~1000만 행 기준 INSERT/SELECT/JOIN/인덱스 크기
   - 한국어 시장에 실측 수치 글 거의 없음
2. **"Hibernate 7에서는 UUID Converter 안 만들어도 됩니다"**
   - `@JdbcTypeCode(SqlTypes.BINARY)` 소개
   - 낡은 가이드 비판적 읽기
3. **"BINARY(16)이 빠르긴 한데 — 실무에서 정말 써야 하나?"**
   - 이론 vs 실무 간극
   - 팀 성숙도, 운영 편의, ROI 관점
4. **"가이드 문서 맹신하지 않기 — 프레임워크 버전별 재평가의 기술"**
   - 메타 글. SB3 시절 패턴을 SB4에서 재검토한 과정

### 인사이트/함정

- **오해 1**: "UUID v7을 BIGINT로 저장?" — 불가. UUIDv7은 128bit, BIGINT는 64bit. TSID만 BIGINT로 가능.
- **오해 2**: "VARCHAR가 문자 범위 넓어 효율적" — UUID는 hex만 쓰므로 해당 안 됨. VARCHAR(36)이 BINARY(16)보다 2.25배 큼.
- **오해 3**: "결국 다 바이너리니까 무관" — 맞음. 단 "무엇의 바이너리"가 다름. "255" 문자열(3 bytes) vs TINYINT 255(1 byte).
- **인사이트**: `autoApply=true` Converter는 `@Id` 필드에 적용 안 될 수 있음. 필드별 `@Convert` 명시가 안전.
- **인사이트**: InnoDB secondary index는 PK를 값으로 복제 → PK 크기 차이가 인덱스 N개만큼 증폭됨.
- **인사이트**: Discord/Instagram이 ID 전략 바꾼 이유도 결국 인덱스 캐시 적중률.

### 코드 포인터

- `shared-kernel/src/main/java/dev/gukin/ehrlab/shared/id/Ids.java`
  - `generate()` — UUIDv7 생성 (`UuidCreator.getTimeOrderedEpoch()`)
  - `toBytes(UUID)` — 16바이트 변환 (ByteBuffer putLong x2)
  - `fromBytes(byte[])` — 복원
- 엔티티 매핑 패턴 (추후 STEP 5 Employee에서): `@JdbcTypeCode(SqlTypes.BINARY) + @Column(columnDefinition = "BINARY(16)")`
- `docs/hr-saas-guide.md` Section 3-6 — 원래 가이드 (참고용)
- 예정 벤치마크: Phase 4~5에 별도 브랜치

---

## 2026-04-21 — Spring Batch 도입 시기 예약

### 맥락
Phase 0 STEP 3 진행 중 "Spring Batch 넣을 수 있나?" 질문 제기. HR 도메인의 배치 친화성 고려하면 필수, 단 타이밍이 관건.

### 결정
- **지금 도입 X** — Phase 0은 shared-kernel 구축 포커스. Spring Batch 복잡도 추가하면 흐름 흐트러짐.
- **Phase 3 진입 전 ADR 작성 후 도입** — 첫 job은 "연차 일괄 부여" (로직 단순 + 근기법 명문화 + 블로그 가치).
- **구조는 ADR에서 결정** — 유력: 각 모듈 내부에 batch 서브패키지 (모듈러 모놀리스 정체성 유지).

### 잠재 배치 job 목록 (Phase별 배치)

| 배치 | 예상 Phase | 주기 |
|---|---|---|
| 연차 일괄 부여 | Phase 3 | 매년 1/1 새벽 |
| 연차 소멸 처리 | Phase 3 | 매일 자정 |
| 근태 일마감 집계 | Phase 4 | 매일 새벽 |
| 월마감 정산 | Phase 4 | 매월 말일 |
| 퇴사자 연차 정산 | Phase 5 | 퇴사일+1 |
| Outbox relay publisher | Phase 3 직전 | 5초 간격 |
| 개인정보 보존기간 삭제 | Phase 5 | 매일 |

### 블로그 각도

- 👑 **"Spring Batch로 근로기준법 기반 연차 자동 부여 구현기"** — 한국어 검색 희소
- "Outbox Relay를 Spring Batch로 구현"
- "배치 Job의 멱등성 — 중복 실행 방어 패턴"
- "근태 월마감 배치 — 대량 집계 성능 튜닝기"

### 체크포인트
Phase 2 완료 시 이 엔트리 다시 읽고 ADR(`docs/adr/NN_batch_module.md`) 작성.

---

## 2026-04-22 — 출근 자격(Attendance Eligibility) 도메인 분리 결정

### 맥락
"인사에 등록되었으면 출근할 수 있어야 하나? 권한 관리는 어떻게 하나?" 질문 제기.
사용자 실무 사례 — 현 회사에서 **발령 이벤트가 근태 등록 트리거**라 발령 전 출근 케이스가 막혀 이슈 제보 자주 들어옴.

### 핵심 통찰
사용자 회사의 함정: **"직원 등록 = 출근 자격"으로 1:1 묶어버린 것.**
운영 현실에선 별개 개념일 수 있음:
- **Identity (신원)**: "우리 직원이다" — 인사 마스터 등록
- **Assignment (발령)**: "어디 소속이다" — 행정 처리 단위
- **Attendance Eligibility (근태 자격)**: "지금 출근 기록 만들 수 있다" — 운영 단위

행정 단위와 운영 단위를 같은 게이트로 묶으면 행정 처리 지연이 모든 운영을 막음.

### 결정 — Policy Resolver 패턴 (가이드 3-7과 일관)

**인사 모듈이 출근 자격 정책의 SSOT.** 근태는 결과만 받음.

```java
// 인사 api
EmployeeService.resolveEligibility(employeeId, when)
  → AttendanceEligibility(eligible, workplaceId, reason)

// 근태
checkIn(employeeId) {
    if (!hrService.resolveEligibility(...).eligible()) reject()
}
```

비즈니스 룰은 인사 모듈 내부에서 자유 조립:
- ACTIVE + 발령 있음 → OK (정상 경로)
- ACTIVE + 발령 없음 + 사전 출근 허가 → OK (예외 경로, 임시 사업장)
- ON_LEAVE / RESIGNED → 거부

### 통신 패턴

| 정보 | 동기화 | 이유 |
|---|---|---|
| 직원 기본 정보 (read model) | 이벤트 | eventual OK, 빈번 조회 |
| **출근 자격 결정** | **API 동기 호출** | 강한 일관성, 정책 변경 즉시 반영 |

→ **데이터 sync는 이벤트, 의사결정은 API.** 둘 다 활용.

### 함정 경고 (체크리스트)

- [ ] 자격 정책을 근태가 소유 X (인사가 통제)
- [ ] eligibility 결과 캐싱 X (출근마다 새로 조회 — 그 사이 퇴사 가능)
- [ ] "인사 등록 = 자동 출근 가능"으로 단축 X (명시적 게이트)

### 블로그 각도

- 👑 **"인사 등록 = 출근 자격? 우리가 분리한 이유"** — 사용자 회사 안티패턴 사례 + 우리 설계 비교
- "Modular Monolith에서 정책의 위치 — Policy Resolver 패턴"
- "발령 행정과 운영 자격을 분리하는 도메인 모델링"

### 코드 포인터 (예정)
- Phase 2 시작 시 ADR 작성: `docs/adr/02_attendance_eligibility_policy.md`
- `modules/hr/api/EmployeeService.resolveEligibility()` 설계
- `modules/hr/domain/AttendanceEligibility` 도메인 객체

### 체크포인트
Phase 2 인사 모듈 진입 시 이 엔트리 + ADR로 정식 의사결정.
