# AI Agent Context Migration Document
> 이 문서는 기존 프로젝트에서 논의된 '연차(Annual Leave) 도메인' DDD 포트폴리오 설계를 **새로운 Playground 프로젝트**로 이관하기 위한 컨텍스트 정의서입니다.
> AI 에이전트는 이 문서를 읽고, 이전 대화의 맥락을 완벽히 복원하여 작업을 이어가야 합니다.

---

## 1. 프로젝트 목표 (Goal)
*   **핵심:** 단순한 비즈니스 로직 구현을 넘어, **백엔드 엔지니어링의 깊이(Deep Dive)**를 탐구하는 Playground 구축.
*   **기술적 지향점:**
    *   **DDD(Domain-Driven Design):** 도메인 로직의 격리, 풍부한 모델링, 바운디드 컨텍스트 적용.
    *   **High Performance:** 대용량 데이터(10만 건 이상) 처리, 동시성 제어(Lock, Redis), JVM/GC 튜닝 실험.
    *   **Architecture:** 멀티 모듈, 이벤트 기반 아키텍처 등 확장성 있는 구조 실험.

## 2. 도메인 정의: 연차 (Annual Leave)
*   **범위:** 연차의 **생성(Grant)**, **사용(Usage)**, **정산(Settlement)**.

### A. 연차 생성 (Grant)
1.  **1년 미만:** 1개월 개근 시 1일 부여 (최대 11일).
    *   *Constraint:* 근태(개근 여부)는 외부 인터페이스로 격리 (`WorkConditionChecker`).
2.  **1년 이상 (회계연도):** 매년 1월 1일 일괄 부여 (기본 15일).
    *   *Calculation:* 첫해 1월 1일은 `(전년도 재직일수 / 365) * 15` 비례 계산.
    *   *Add-on:* 3년 근속 시 2년마다 +1일 (Max 25일).

### B. 연차 사용 (Usage)
1.  **선차감(Hold) 모델:** 신청 시 즉시 차감되지 않고 '사용 예정(Pending)' 상태로 확보 -> 승인 시 '확정(Deducted)'.
2.  **FIFO 차감:** 여러 개의 연차(작년 이월분, 올해 발생분) 보유 시, 유효기간이 임박한 연차부터 자동으로 차감.
3.  **단위:** 1일, 0.5일(반차), 0.25일(반반차).

### C. 연차 정산 (Settlement)
1.  **만료/퇴사:** 유효기간 만료 또는 퇴사 시 잔여 연차 정산.
2.  **연차 촉진:** 적법한 촉진 절차를 거친 경우 금전 보상 의무 면제(소멸) 로직 구현 필요.

## 3. 추천 패키지 구조 (Standard DDD)
```text
com.example.annualleave  <-- Root Package
├── api           (Presentation Layer: DTO, Controller)
├── application   (Application Layer: Service, Facade, Usecase)
├── domain        (Domain Layer: Entity, VO, Aggregate, Repository Interface)
└── infra         (Infrastructure Layer: JPA Impl, External Adaptors)
```

## 4. 향후 로드맵 (Playground Scenario)
1.  **Phase 1:** 순수 DDD 기반의 핵심 도메인 로직 구현 및 단위 테스트 (현재 단계).
2.  **Phase 2:** 대용량 더미 데이터(직원 10만 명) 생성기 구현.
3.  **Phase 3:** 동시성 테스트 (Lock, Redis), 배치 성능 튜닝, 인덱싱 최적화 등 심화 실험.

## 5. 문서 관리 표준 (Documentation Standard)
프로젝트의 실험 과정과 의사결정을 체계적으로 기록하기 위해 아래의 폴더 구조를 따른다.
*   **규칙:** 폴더에는 대분류 번호(00, 10...)를 붙이고, 파일은 폴더 내에서 순차 번호(01, 02...)를 매긴다.

```text
docs/
├── 00_Onboarding/           # 프로젝트 진입점 & 컨텍스트
│   ├── 01_context_migration.md  <-- 본 문서 (이동 예정)
│   └── 02_setup_guide.md
│
├── 10_Requirements/         # 비즈니스 요구사항 & 도메인 분석
│   ├── 01_domain_rules.md       <-- 도메인 규칙 정의서 (이동 예정, 구 domain_analysis_annual_leave.md)
│   └── 02_glossary.md           <-- 보편 언어/용어 사전
│
├── 20_System_Design/        # 아키텍처 & 설계도
├── 30_DDD_Model/            # 도메인 모델링 상세
├── 90_Engineering_Log/      # ★ Playground 핵심: 실험 및 튜닝 일지
└── 99_ADR/                  # Architecture Decision Records
```

## 6. 협업 프로토콜 (Collaboration Protocol)
**사용자의 학습과 포트폴리오 효과를 극대화하기 위해 AI Agent는 '작성자(Driver)'가 아닌 '리뷰어(Reviewer)' 역할을 수행한다.**

*   **Role: User (Driver)**
    *   설계, 코드 작성(Entity, Service, Test), 리팩토링을 주도적으로 수행한다.
    *   새 프로젝트 생성 및 문서 이관 작업을 수행한다.
*   **Role: AI Agent (Senior Reviewer)**
    *   사용자가 작성한 코드를 분석하고 피드백을 제공한다 (DDD 원칙 준수 여부, 동시성 이슈 가능성, 성능 개선 포인트 등).
    *   직접적인 코드 수정(`write_file`, `replace`)이나 구현을 **지양**한다.
    *   질문을 통해 사용자가 스스로 답을 찾도록 유도한다 (Socratic Method).
    *   *Exception:* 초기 환경 세팅을 위한 쉘 스크립트 작성이나, 문서 템플릿 생성 등 반복적이고 학습 가치가 낮은 작업은 지원 가능하다.

---
**[Action Item for Agent]**
*   이 문서를 읽은 후, 사용자가 작성해 올 `AnnualLeave` 엔티티 코드를 기다린다.
*   코드가 제출되면 DDD 관점(Aggregate Root의 적절성, 비즈니스 로직의 위치 등)에서 엄격하게 리뷰한다.