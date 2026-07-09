# 테스트 결과

이 문서는 `claude/attachment-review-ugptk9` 브랜치에 스캐폴딩한 Android 프로젝트를 실제로
검증하기 위해 수행한 테스트와 그 결과를 정리한 것이다.

## 환경 제약 (먼저 확인한 것)

이 작업을 수행한 샌드박스는 다음이 막혀 있어 **정상적인 `./gradlew assembleDebug`를 끝까지
실행할 수 없다**:

| 확인 대상 | 결과 |
|---|---|
| `dl.google.com` (AGP, AndroidX 저장소) | 프록시에서 403(정책 차단) |
| `services.gradle.org` (Gradle 배포판) | 최종적으로 403 |
| `github.com` (릴리스 바이너리) | 403 |
| `repo1.maven.org` (Maven Central) | **정상 접근 가능** |

AGP(`com.android.application`)와 AndroidX 계열(`androidx.*`, Compose, Room, WorkManager 등)은
Maven Central에 미러링되어 있지 않음을 직접 확인했다(`com.android.tools.build:gradle`,
`androidx.compose.ui:ui` 모두 404). 즉 **Gradle을 통한 전체 빌드 검증은 이 환경에서 원천적으로
불가능**하다 — 우회 수단이 아니라 실제 네트워크 정책 제약이다.

## 그래서 실제로 무엇을 어떻게 검증했나

Maven Central은 열려 있으므로, Kotlin 자체(`org.jetbrains.kotlin:kotlin-compiler`)와 순수 JVM
라이브러리(kotlinx-serialization, kotlinx-coroutines)는 내려받을 수 있었다. 이를 이용해 두 단계로
검증했다.

### 1. 순수 로직 모듈 — 실제 컴파일 + 실행 테스트

Android 프레임워크에 의존하지 않는 파일들(`data/model/*`, `prompt/PromptBuilder.kt`,
`prompt/ResponseParser.kt`, `prompt/PurposePromptTemplates.kt`, `prompt/MemoIntent.kt`)을
프로젝트와 **동일한 Kotlin 1.9.24 컴파일러 + kotlinx-serialization 컴파일러 플러그인**으로 직접
컴파일했다.

**1차 시도에서 실제 컴파일 에러 2건을 발견 → 수정함** (`ResponseParser.kt`):

1. `crossinline onRawTextFallback: suspend (String) -> Unit = {}` — inline 함수의 suspend
   람다 파라미터는 기본값을 가질 수 없다는 Kotlin 컴파일러 제약에 위반. 기본값을 제거하고
   호출부(`IntentClassifier`, `GlossaryScanner`)에서 명시적으로 전달하도록 수정.
2. `private val json = Json {...}` — public inline 함수(`parseWithRetry`) 내부에서 같은
   클래스의 `private` 멤버에 접근할 수 없음. `@PublishedApi internal`로 변경.

수정 후 재컴파일 → **에러 0건, 경고 0건**으로 정상 컴파일 확인.

이어서 컴파일된 클래스를 실제로 실행하는 25개 스모크 테스트를 작성해 돌렸다(`runBlocking`으로
suspend 함수까지 포함):

```
PASS: extractJsonBlock: plain object
PASS: extractJsonBlock: fenced block
PASS: extractJsonBlock: prose-wrapped, no fence
PASS: extractJsonBlock: nested braces balance correctly
PASS: extractJsonBlock: array
PASS: parseWithRetry: succeeds on 2nd attempt
PASS: parseWithRetry: no fallback logged on eventual success
PASS: parseWithRetry: returns null after exhausting attempts
PASS: parseWithRetry: fallback receives the last raw text
PASS: GlossaryScanResult: parses terms list
PASS: PromptBuilder: 5 purposes all produce prompts
PASS: PromptBuilder: structure instructions differ across purposes
PASS: PromptBuilder: STT-correction note present in every prompt
PASS: PromptBuilder: project name interpolated
PASS: PromptBuilder: glossary terms embedded
PASS: PromptBuilder: existing doc content embedded
PASS: IntentClassificationPrompt: contains the utterance
PASS: IntentClassificationPrompt: mentions glossary_command
PASS: Purpose.default is CODING_PLAN (matches mockup pre-selection)
PASS: Purpose.fromId round-trips
PASS: Purpose.fromId falls back to default for unknown id
PASS: DocType.fromId round-trips
PASS: DocType.fromId returns null for unknown id
PASS: CODING_PLAN default docs include todo+architecture+decisions (matches todo.md Phase 1-1)
PASS: TASK_PLANNING default docs are checkbox-only (todo.md only)

== SUMMARY: 25 passed, 0 failed ==
```

무엇을 검증했는지: JSON 추출(순수 텍스트/코드펜스/설명 섞인 텍스트/중첩 중괄호/배열 5가지 케이스),
파싱 실패 시 재생성-재시도 로직과 원문 폴백 콜백, 목적(Purpose)별 프롬프트가 서로 다른 지시문을
포함하는지, 용어집·기존 문서 내용이 프롬프트에 실제로 삽입되는지, `Purpose`/`DocType`의
id round-trip과 기본값(`todo.md` 문서 구조 요구사항과 일치) 등.

### 2. 전체 53개 소스 파일 — 컴파일러 기반 "내부 배선" 검사

Android SDK 없이 전체 앱 소스를 같은 컴파일러로 돌리면 `androidx`/`android`/`kotlinx.coroutines`
등 미해결 참조 에러가 대량(1,299건) 발생하는 게 당연하다 — 이 환경엔 그 라이브러리들이 없기
때문이다. 하지만 이 로그는 그 자체로 유용한 차등 검사가 된다: **우리 자신의 클래스/메서드 이름이
에러 메시지에 등장하면 실제 배선 버그(생성자 인자 불일치, 오타 등)일 가능성이 높다.**

고유 에러 메시지 179개를 추출해 `wemeet`/`ProjectRepository`/`DocumentManager`/
`LlmSessionManager`/`PromptBuilder`/`ResponseParser`/`StorageManager`/`GlossaryScanner`/
`IntentClassifier`/`AppContainer` 등 우리 자신의 심볼명을 grep했다 — **일치하는 항목 0건**.
일반적이지 않아 보였던 에러 2건(`no value passed for parameter 'value'`,
`receiver type mismatch`)도 직접 추적해보니 각각 `androidx.work.ListenableWorker.Result`와
`androidx.compose.ui.text.font.FontFamily`가 클래스패스에 없어서 컴파일러가 `kotlin.Result` 등
엉뚱한 후보로 잘못 추론한 연쇄 오류였을 뿐, 실제 코드 버그가 아님을 확인했다.

### 3. 빌드 설정 파일 — 프로그래밍적 크로스체크

- `gradle/libs.versions.toml`을 Python `tomllib`로 파싱 → 문법 오류 없음(versions 17,
  libraries 26, plugins 4).
- `app/build.gradle.kts`·루트 `build.gradle.kts`에서 쓰인 `libs.*`/`libs.plugins.*` 접근자
  35개를 모두 추출해 버전 카탈로그에 실제로 존재하는지 대조 → **누락 0건**.
- `AndroidManifest.xml` 및 `res/**/*.xml` 전체를 `xml.dom.minidom`으로 파싱 → 모두 정상.
- 코드에서 참조하는 `R.string.*`/`R.drawable.*` 4곳을 `strings.xml` 실제 내용과 대조,
  `android.R.drawable.*`(프레임워크 리소스)와 앱 자체 `R.string.*`이 혼동 없이 올바르게
  분리되어 있음을 확인.

## 결과 요약

| 항목 | 결과 |
|---|---|
| 순수 로직 9개 파일 실제 컴파일 | ✅ (버그 2건 발견 → 수정 → 재검증 통과) |
| 순수 로직 런타임 스모크 테스트 | ✅ 25/25 통과 |
| 전체 53개 파일 컴파일러 기반 내부 배선 검사 | ✅ 우리 자신의 심볼을 원인으로 하는 에러 0건 |
| Gradle 버전 카탈로그 ↔ 빌드 스크립트 정합성 | ✅ 35/35 접근자 일치 |
| XML 리소스 well-formed 여부 | ✅ 전체 통과 |
| R.* 리소스 참조 정합성 | ✅ 전체 통과 |
| **실제 `./gradlew assembleDebug`** | ❌ 이 샌드박스에서 Android SDK/AGP를 받을 수 없어 실행 불가 |
| UI 실제 렌더링(Compose 미리보기/에뮬레이터) | ❌ 미실행 |
| 오버레이 창/마이크 권한/MediaPipe 추론 실기기 동작 | ❌ 미실행 — 로직은 공개 문서 기준으로 작성, 실기기 확인 필요 |

위 25개 스모크 테스트는 일회성 스크립트로 버리지 않고, 실제 Gradle 빌드가 가능해지면 그대로
돌아가는 JUnit4 테스트 20개로 옮겨 커밋했다: `app/src/test/java/.../prompt/ResponseParserTest.kt`,
`prompt/PromptBuilderTest.kt`, `data/model/PurposeAndDocTypeTest.kt`. 이 20개 테스트는 JUnit
4.13.2로 실제 실행까지 확인했다:

```
JUnit version 4.13.2
....................
Time: 0.133

OK (20 tests)
```

Android Studio에서는 `./gradlew testDebugUnitTest`로 실행하면 된다.

## 결론

- **실제로 버그가 있었고, 실제로 고쳤다.** `ResponseParser.kt`의 inline 함수 관련 컴파일 에러
  2건은 이 검증을 하지 않았다면 Android Studio를 처음 여는 시점에야 발견됐을 것이다.
- Kotlin 언어 레벨 정합성(문법, 타입, inline/suspend 규칙)과 우리 자신의 클래스 간 배선은
  이번 검증으로 상당히 신뢰할 수 있다.
- 다만 **Android 프레임워크·Compose·Room·WorkManager·MediaPipe API를 실제로 올바르게 호출하고
  있는지, UI가 목업처럼 그려지는지, 오버레이/음성/온디바이스 LLM이 실기기에서 실제로 동작하는지는
  전혀 검증되지 않았다.** README의 "알려진 제약" 절과 동일하게, Android Studio에서 한 번 열어
  동기화·빌드하고 실기기(또는 에뮬레이터)에서 돌려보는 과정이 반드시 필요하다.
