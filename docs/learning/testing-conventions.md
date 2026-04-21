# 테스트 작성 컨벤션

> 이 프로젝트의 테스트 작성 기준. 스타일 + 판단 기준.
> `Ids` 클래스의 스모크 테스트 작성 과정에서 정리.

## 목차

1. [테스트 종류와 목적](#1-테스트-종류와-목적)
2. [무엇을 테스트할지 판단 기준](#2-무엇을-테스트할지-판단-기준)
3. [AssertJ vs JUnit Jupiter Assertions](#3-assertj-vs-junit-jupiter-assertions)
4. [AAA 패턴 (Arrange-Act-Assert)](#4-aaa-패턴)
5. [테스트 이름 짓기](#5-테스트-이름-짓기)
6. [주의할 함정](#6-주의할-함정)

---

## 1. 테스트 종류와 목적

| 종류 | 무엇을 검증 | 깊이 | 예시 |
|---|---|---|---|
| **Smoke** | "죽지 않음" 수준의 기본 동작 | 얕음 | round-trip 변환, 앱 부팅, /health 200 |
| **Unit** | 함수/클래스 단위 격리 검증 | 중간 | 다양한 입력·엣지 케이스 |
| **Integration** | 여러 컴포넌트 결합 | 깊음 | Service + Repository + DB |
| **E2E** | 사용자 시나리오 전체 | 가장 깊음 | 로그인 → 직원 등록 → 조회 |

**어원:** Smoke test는 전자제품 만들고 첫 전원 켰을 때 "연기 안 나면 통과"에서 유래. 가장 기본적인 안전망.

**실무 감각:** 유틸 클래스 정도는 smoke + 핵심 unit 2~3개로 충분. 복잡한 도메인 로직은 더 깊은 unit + integration.

---

## 2. 무엇을 테스트할지 판단 기준

### 핵심 원칙 — "라이브러리 vs 내 코드"

**라이브러리가 보장하는 건 재검증하지 않는다.**

예: `UuidCreator.getTimeOrderedEpoch()`이 RFC 9562 UUIDv7을 만든다는 사실은 uuid-creator 라이브러리 책임. 그쪽 테스트 스위트가 검증. 우리가 매번 확인할 일 아님.

**내가 짠 로직만 검증한다.**

예: `toBytes`/`fromBytes`의 변환 정확성, null 처리 분기, 우리 쪽에서 정의한 계약.

### 테스트 가치 매트릭스 (예: `Ids` 클래스)

| 검증 후보 | 누구 책임 | 가치 | 채택 |
|---|---|---|---|
| `generate()`가 v7 형식 | 라이브러리 | 낮음 | ❌ |
| `toBytes` → `fromBytes` round-trip | 우리 코드 | 높음 | ✅ |
| null 입력 안전 | 우리 코드 | 높음 | ✅ |
| UUIDv7 시간순 정렬 특성 | (경계적) | 학습·문서 가치 | ✅ |
| 동시 생성 시 충돌 | 라이브러리 | 낮음 | ❌ |
| 극단값 (zero/max UUID) | 우리 코드 | 낮음 (실무 시나리오 없음) | ❌ |

### 방어적 테스트 함정

**"잘못된 입력에 대한 방어"가 필요한가?** 호출 경로 전체를 우리가 통제한다면 NO (YAGNI). 외부 경계에서만 검증.

---

## 3. AssertJ vs JUnit Jupiter Assertions

### 기본 결론

**AssertJ를 기본으로 사용.** 한국 모던 팀(토스/우아한/당근 등) 주류.

### 비교

| | JUnit Jupiter | AssertJ |
|---|---|---|
| 스타일 | `assertEquals(expected, actual)` | `assertThat(actual).isEqualTo(expected)` |
| 체이닝 | ❌ | `.isNotNull().hasSize(16)` |
| 에러 메시지 | 기본적 | 풍부 (타입 정보, 차이 표시) |
| 컬렉션 검증 | 제한적 | 강력 (`.contains`, `.extracting` 등) |

### ⚠️ `isEqualTo` 방향 — 자주 틀림

**AssertJ 관례: 좌측에 "검증 대상(actual)", 우측에 "기대값(expected)".**

```java
// ❌ 뒤집힘 (동작은 하나 에러 메시지 오독 유발)
assertThat(original).isEqualTo(actual);

// ✅ 올바른 방향
assertThat(actual).isEqualTo(original);
```

JUnit의 `assertEquals(expected, actual)` 순서와 **반대**라 헷갈리기 쉬움. 실패 시 에러 메시지는:

```
Expecting actual:   X
to be equal to:     Y
```

방향 뒤집히면 "기대값이 실제와 다르다"로 읽혀 디버깅 어려움.

### `assertAll` vs `SoftAssertions`

복수 assertion을 한 번에 검증할 때:

```java
// JUnit Jupiter — 섞어쓰기 OK
assertAll(
    () -> assertThat(uuid).isNull(),
    () -> assertThat(bytes).isNull()
);

// AssertJ 순수 스타일
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(uuid).isNull();
    softly.assertThat(bytes).isNull();
});
```

둘 다 허용. 스타일 통일만 지키면 됨.

---

## 4. AAA 패턴

각 테스트 내부를 3구역으로 나누고 **빈 줄로 분리**:

```java
@Test
void toBytes_fromBytes_equals() {
    // Arrange — 입력 준비
    UUID original = Ids.generate();

    // Act — 검증 대상 실행
    UUID actual = Ids.fromBytes(Ids.toBytes(original));

    // Assert — 결과 확인
    assertThat(actual).isEqualTo(original);
}
```

### 효과

- 테스트 의도가 한눈에 들어옴
- 어디서 실패했는지 파악 쉬움
- 복붙 리뷰에도 도움

### 스타일 팁

- 주석 `// Arrange ...` 는 **생략해도 OK** (빈 줄이 분리 표시)
- 한 구역이 여러 줄이어도 빈 줄로 다음 구역과 분리
- 너무 복잡해지면 helper method 추출

---

## 5. 테스트 이름 짓기

### 클래스 레벨

```java
@DisplayName("식별자 스모크 테스트")
class IdsTest { }
```

- 한글 `@DisplayName` 권장 (가독성)
- 클래스 이름은 영어 `{ClassName}Test` 관례

### 메서드 레벨

```java
@Test
@DisplayName("바이트 변환 결과가 동일하다")
void toBytes_fromBytes_equals() { }
```

**두 가지 스타일 다 허용:**

| 스타일 | 예시 |
|---|---|
| 영어 + snake/camel | `toBytes_fromBytes_equals()` 또는 `shouldReturnNull_whenNullGiven()` |
| `@DisplayName` 한글 | `"null이 주어졌을 때 null을 반환한다"` |

**추천 조합:** 메서드는 영어(IDE 네비게이션 수월), `@DisplayName`은 한글(리포트 가독성). 토스/우아한 주류.

### 메서드명 패턴

- `{대상}_{조건}_{기대결과}` (예: `toBytes_nullInput_returnsNull`)
- `should{기대}_when{조건}` (예: `shouldReturnNull_whenNullGiven`)
- 본인 취향으로 한 가지 정하고 프로젝트 일관성 유지

---

## 6. 주의할 함정

### 함정 1 — 같은 ms 내 flaky test

UUIDv7은 밀리초 단위 timestamp. 연속 호출이 같은 ms 안에 들어가면 tiebreaker에 의존.

```java
UUID uuid1 = Ids.generate();
UUID uuid2 = Ids.generate();      // ms가 같을 수 있음!
assertThat(uuid1).isLessThan(uuid2);   // flaky 위험
```

**안전:**
```java
UUID uuid1 = Ids.generate();
Thread.sleep(2);                  // ms 경계 확실히 넘김
UUID uuid2 = Ids.generate();
```

uuid-creator는 같은 ms 내에서도 monotonic 증가 보장하지만, **의도 전달** 측면에서 `sleep` 추가가 낫다.

### 함정 2 — 라이브러리 동작을 재검증

```java
// ❌ 의미 없음 — 라이브러리가 보장하는 것
assertThat(uuid.version()).isEqualTo(7);
```

uuid-creator 자체 테스트에서 검증됨. 우리가 매번 할 일 아님. 테스트가 많아지면 유지비만 늘어남.

### 함정 3 — 와일드카드 import

```java
import static org.junit.jupiter.api.Assertions.*;   // 위험
```

무엇이 import됐는지 불분명. 이름 충돌 가능. **필요한 것만 명시:**

```java
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.assertj.core.api.Assertions.assertThat;
```

### 함정 4 — "방어적 프로그래밍 과잉"

우리가 호출 경로를 통제하는 곳까지 방어 로직을 쓰고 테스트하는 것.

예: `fromBytes`에 16 바이트 아닌 입력이 들어올 가능성이 없다면 → IllegalArgumentException 검증은 불필요. 외부 경계에서만 검증.

---

## 빠른 체크리스트

새 테스트 작성 시:
- [ ] 라이브러리 책임 아니라 **내 코드 로직**을 검증하는가?
- [ ] **AAA 패턴**으로 3구역 분리됐는가?
- [ ] `assertThat(actual).isEqualTo(expected)` **방향**이 맞는가?
- [ ] `@DisplayName`으로 **의도**가 한글로 표현됐는가?
- [ ] 시간 의존 / 순서 의존 / 환경 의존 **flaky 요소** 없는가?

---

## 변경 이력

| 날짜 | 변경 내용 |
|---|---|
| 2026-04-22 | 초안 — `IdsTest` 작성 과정에서 정리 |
