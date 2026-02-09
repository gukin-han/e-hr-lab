# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

e-hr-lab은 연차 관리 시스템을 위한 **DDD (Domain-Driven Design) Playground**입니다. DDD 모델링, 고성능 패턴, 동시성 테스트 등 백엔드 엔지니어링의 깊이를 탐구합니다.

**기술 스택:**
- Spring Boot 4.0.0 + Java 21
- H2 Database (인메모리)
- Virtual Threads (Project Loom) 활성화
- k6 부하 테스트

## 빌드 및 실행 명령어

```bash
# 애플리케이션 실행 (포트 9090)
./gradlew bootRun

# 전체 테스트 실행
./gradlew test

# 전체 빌드 (컴파일 + 테스트)
./gradlew clean build

# 부하 테스트 (k6 설치 필요)
k6 run k6/results/io-test.js
k6 run k6/results/cpu-test.js
```

## 아키텍처

### 도메인 모델 (DDD Aggregates)

핵심 도메인은 **연차(Annual Leave)**이며, 다음과 같은 Aggregate 구조를 가집니다:

```
LeaveAccount (Aggregate Root - 연차 통장)
├── LeaveGrant (부여 내역) - 유효기간이 있는 연차 자산
├── LeaveRequest (연차 신청) - 상태 워크플로우를 가진 사용자의 휴가 신청
│   └── LeaveUsage (차감 상세) - 어떤 Grant에서 얼마를 차감했는지 기록
```

**핵심 비즈니스 규칙:**
- **FIFO 차감:** 만료일이 빠른 Grant부터 우선 소진
- **선차감(Hold) 모델:** 신청 시 즉시 예약(PENDING) → 승인 시 확정(APPROVED)
- **Request/Usage 분리:** Request는 상태 관리, Usage는 실제 차감 내역 추적 (롤백 지원)

### 패키지 구조 (목표)

```
ehrlab/
├── api/          # Presentation Layer (DTO, Controller)
├── application/  # Application Layer (Service, Usecase)
├── domain/       # Domain Layer (Entity, VO, Aggregate, Repository Interface)
└── infra/        # Infrastructure Layer (JPA Impl, External Adaptors)
```

### 문서 구조

```
docs/
├── 00_Onboarding/      # 프로젝트 컨텍스트 및 설정
├── 10_Requirements/    # 도메인 규칙 및 용어 사전
├── 30_DDD_Model/       # Aggregate 정의
├── 90_Engineering_Log/ # 성능 실험 및 학습 기록
└── 99_ADR/             # Architecture Decision Records
```

## AI 에이전트 협업 프로토콜

**중요: AI는 코드 작성자가 아닌 시니어 리뷰어 역할을 수행합니다.**

- 사용자가 설계, 구현, 리팩토링을 주도
- AI는 DDD 원칙, 동시성 이슈, 성능 관점에서 피드백 제공
- 소크라테스식 문답법 사용: 직접적인 답변보다 질문을 통해 유도
- **예외:** 인프라 스크립트, 문서 템플릿 등 학습 가치가 낮은 작업은 AI가 직접 작성 가능

이 프로토콜은 학습 효과와 포트폴리오의 진정성을 극대화하기 위함입니다.

## 도메인 컨텍스트

**연차 생명주기:**
1. **생성(Grant):** 1년 미만자는 입사 시 선생성, 1년 이상자는 매년 1월 1일 일괄 부여
2. **사용(Usage):** 신청 → 선차감(PENDING) → 승인(APPROVED), FIFO 방식으로 차감
3. **정산(Settlement):** 유효기간 만료 또는 퇴사 시 보상/소멸 처리

**사용 단위:** 1.0일, 0.5일(반차), 0.25일(반반차    )
