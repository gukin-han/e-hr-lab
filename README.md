# e-hr-lab

한국형 HR B2B SaaS를 **모듈러 모놀리스**로 설계·구현하는 학습 프로젝트.
설계 의사결정 과정과 실무 감각 축적이 목표. 진행 내용은 블로그 시리즈로 발행 예정.

## 기술 스택

- **언어/런타임**: Java 21
- **프레임워크**: Spring Boot 4.0.5 (Spring Framework 7, Jakarta EE 11)
- **빌드**: Gradle 9.4.1, Kotlin DSL, buildSrc convention plugin
- **DB**: MySQL 8.4 (4개 DATABASE로 모듈 경계 분리)
- **ID**: UUIDv7 + BINARY(16)
- **기타**: Flyway, Hibernate 7, JUnit 5, AssertJ

## 실행

```bash
# MySQL + Adminer 기동 (포트: MySQL 13306, Adminer 8081)
docker compose up -d --wait

# 전체 빌드
./gradlew build

# 특정 모듈 테스트
./gradlew :shared-kernel:test
```

## 모듈 구조

```
e-hr-lab/
├── app/                 # Spring Boot 실행 모듈 (Composition Root)
├── shared-kernel/       # 공통 인프라 (Outbox, Tenant, UUID 등)
└── modules/
    ├── hr/              # 인사 도메인 (SSOT)
    ├── attendance/      # 근태 도메인
    └── leave/           # 휴가 도메인
```

## 문서

### 설계 · 로드맵

1. [HR SaaS 설계·구현 가이드](docs/hr-saas-guide.md) — 프로젝트 전체 로드맵, Phase 0~6 계획, 기술 의사결정

### 진행 중 기록

2. [엔지니어링 로그](docs/engineering-log.md) — Phase 진행 중 의사결정과 트레이드오프 기록. Phase 종료 시 블로그 초안 재료.

### 학습 레퍼런스 (`docs/learning/`)

3. [Gradle 멀티모듈 의존성](docs/learning/gradle-multimodule-dependencies.md) — 9개 `.gradle.kts` 파일 지도, `implementation`/`api`/`compileOnly` 차이, 의존성 그래프 시각화
4. [테스트 작성 컨벤션](docs/learning/testing-conventions.md) — AssertJ vs JUnit 선택 기준, AAA 패턴, 테스트 가치 판단, 자주 틀리는 `isEqualTo` 방향

## 진행 상태

**현재 Phase 0 (기반공사) — STEP 3 진행 중**

- ✅ STEP 1: Gradle 멀티모듈 + Spring Boot 4.0.5
- ✅ STEP 2: Docker Compose로 MySQL 4개 DATABASE 기동
- 🔄 STEP 3: shared-kernel 핵심 컴포넌트 구현
  - [x] Ids (UUIDv7 생성 + toBytes/fromBytes + 스모크 테스트)
  - [ ] TenantContext + TenantFilter
  - [ ] CorrelationContext
  - [ ] OutboxWriter ⭐
- ⬜ STEP 4: Flyway로 복수 DATABASE 초기화
- ⬜ STEP 5: 인사 모듈 최소 구현
- ⬜ STEP 6: ArchUnit으로 모듈 경계 강제
- ⬜ STEP 7~9: Testcontainers, GitHub Actions CI, 문서화

**다음 작업:** STEP 3-2 — TenantContext + TenantFilter (ThreadLocal + OncePerRequestFilter 패턴)
