# 빌드 도구 신분증(Group ID) 및 패키지 구조에 대한 학습 기록

**태그:** `Gradle`, `Maven`, `Group ID`, `Package`, `DDD`, `Refactoring`

---

## 1. 학습 배경

기존 Java 프로젝트의 패키지 구조(`com.example.ehrlab`)를 `ehrlab`으로 간소화하는 리팩토링을 진행하던 중, `build.gradle.kts` 파일에 명시된 `group = "com.example"` 속성에 대한 의문이 발생했다. 패키지명과 Group ID의 관계, 그리고 그 목적에 대해 깊이 이해할 필요성을 느꼈다.

---

## 2. 빌드 도구 Group ID (신분증)란?

*   **정의:** Gradle이나 Maven 같은 빌드 도구에서 프로젝트의 **고유한 식별자**를 부여하는 메타데이터의 한 부분이다. 일반적으로 `Group (G)`, `Artifact (A)`, `Version (V)` 세 가지 요소(G.A.V)로 라이브러리/모듈을 식별한다.
    *   `group`: 누가 만들었는지 (예: `com.hangugin`, `org.springframework`)
    *   `artifact (name)`: 무엇을 만들었는지 (예: `e-hr-lab`, `spring-boot-starter`)
    *   `version`: 몇 번째 버전인지 (예: `0.0.1-SNAPSHOT`)

*   **주요 사용처:**
    *   **외부 라이브러리 참조:** 다른 프로젝트에서 현재 프로젝트를 라이브러리로 사용할 때, `implementation("com.gukin:e-hr-lab:0.0.1")`와 같은 형식으로 의존성을 추가한다. 이때 `group`이 중요한 식별자로 활용된다.
    *   **원격 저장소 (Repository) 관리:** Maven Central이나 사내 Nexus/Artifactory 같은 아티팩트 저장소에 라이브러리를 배포할 때, `group`은 저장소 내의 디렉토리 구조나 고유 식별자로 사용된다.
    *   **Docker 이미지 태그:** Jib 같은 도구로 Docker 이미지를 생성할 때 이미지 태그의 일부로 활용되기도 한다.

---

## 3. Group ID와 Package 명칭의 관계

*   **전통적인 관례:** Java 개발에서 `Group ID`와 최상위 `Package` 이름을 일치시키는 것이 오랜 관례였다. (예: `group="com.example"`, `package com.example.myproject;`)
*   **실용적 관점:**
    *   **일치해야 할 필요는 없음:** `Group ID`는 주로 빌드 시스템 외부에서의 식별자 역할을 하고, `package`는 Java 코드 내부의 네임스페이스(namespace) 역할을 하므로, 반드시 일치해야 하는 것은 아니다.
    *   **불일치 사례:** `Lombok`(`group: org.projectlombok`, `package: lombok`)처럼 불일치하는 사례도 흔하다.
    *   **현재 프로젝트의 결정:** `ehrlab`이라는 간결한 루트 패키지명을 사용하되, `group`은 `com.gukin`과 같이 개인/조직 식별성을 나타내도록 분리하는 것이 합리적이라고 판단했다.

---

## 4. 리팩토링 결정 및 결과

*   **이전:** `group = "com.example"`, `package com.example.ehrlab;`
*   **변경:** `group = "com.gukin"`, `package ehrlab;`
*   **영향:** Java 코드 내부 구조의 간결성을 유지하면서도, 빌드 아티팩트에는 개인의 고유 식별자를 부여하여 포트폴리오의 명확성을 높였다. 실행 가능한 애플리케이션에서는 `group`이 직접적인 빌드/실행에 영향을 미치지 않음을 확인했다.

---
**기억할 점:**
* `Group ID`는 빌드 도구의 신분증이며, 특히 라이브러리 배포 시 중요.
* `Package` 이름은 Java 코드 내부의 네임스페이스로, 가독성과 관리 용이성을 위해 간결하게 가져갈 수 있다.
* 둘이 반드시 일치할 필요는 없으며, 프로젝트의 목적과 맥락에 맞춰 실용적으로 결정한다.
