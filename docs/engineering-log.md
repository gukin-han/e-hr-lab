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
