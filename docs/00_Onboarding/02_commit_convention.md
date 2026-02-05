# 커밋 컨벤션

이 프로젝트는 **Conventional Commits** 스타일을 따릅니다.

---

## 커밋 메시지 형식

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 예시

```
feat(attendance): 출퇴근 API 구현

- 대규모 트래픽 처리 경험을 위한 출퇴근 기록 API
- Phase 1: 단순 동기 구조로 시작하여 병목 지점 파악 목적
- Transaction Script 패턴 적용 (성능 측정 용이)

Closes #12
```

---

## 구성 요소

### Type (필수)

| Type | 용도 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `test` | 테스트 추가/수정 |
| `refactor` | 기능 변경 없는 코드 개선 |
| `chore` | 빌드, 설정 등 기타 변경 |
| `perf` | 성능 개선 |

### Scope (선택)

도메인 또는 영역을 명시합니다.

| Scope | 대상 |
|-------|------|
| `attendance` | 출퇴근 도메인 |
| `leave` | 연차 도메인 |
| `shared` | 공통 모듈 |
| `deps` | 의존성 |
| `config` | 설정 |

### Subject (필수)

- 명령형, 현재 시제로 작성
- 첫 글자 소문자 (영어의 경우)
- 마침표 없음
- 50자 이내 권장

### Body (권장)

**What(무엇)이 아닌 Why(왜)를 기록합니다.**

- 왜 이 변경이 필요한가?
- 어떤 문제를 해결하는가?
- 어떤 설계 결정을 했는가?

```
# 좋은 예 (Why)
- 대규모 트래픽 처리 경험을 위해 단순 구조로 시작
- DDD 대신 Transaction Script 선택: 성능 병목 관찰 용이

# 나쁜 예 (What만 나열)
- AttendanceService.java 추가
- AttendanceController.java 추가
```

### Footer (선택)

- 이슈 연결: `Closes #123`, `Fixes #456`
- Breaking Change: `BREAKING CHANGE: 설명`

---

## Breaking Change

하위 호환성이 깨지는 변경 시:

```
feat(attendance)!: workDate 계산 로직 변경

BREAKING CHANGE: workDate가 UTC 기반에서 테넌트 타임존 기반으로 변경됨
```

---

## 커밋 단위

- **작고 논리적인 단위**로 커밋
- 하나의 커밋 = 하나의 목적
- 빌드가 깨지지 않는 상태 유지

### 좋은 커밋 단위 예시

```
docs: 출퇴근 서비스 설계 명세서 추가
feat(attendance): 도메인 Enum 정의
feat(attendance): AttendanceRecord 엔티티 구현
feat(attendance): DailyAttendance 엔티티 구현
feat(attendance): Repository 인터페이스 및 구현
feat(attendance): AttendanceService 구현
feat(attendance): 출퇴근 API 엔드포인트 구현
test(attendance): 통합 테스트 추가
```

---

## 참고

- [Conventional Commits](https://www.conventionalcommits.org/)
- [How to Write a Git Commit Message](https://cbea.ms/git-commit/)
