# 프로젝트 메모리 (Project Memory)

음성으로 남긴 아이디어를 온디바이스 LLM(Gemma-4-E4B, LiteRT-LM)이 프로젝트별 Markdown 문서에
자동 반영하는 개인용 Android 앱. 서버/동기화 없음 — STT만 온라인, LLM 추론은 완전히 기기 내부에서
실행된다. 배경과 설계 결정은 `docs/todo.md`(원본 작업 리스트)·`docs/mockup.html`(UI 목업)과 이 문서
하단의 "설계 결정" 절 참고.

이 커밋들은 Phase 1~5 아키텍처 전체를 스캐폴딩한 것이다. UI(Phase 1)는 실제로 동작하는 수준까지
구현했고, 온디바이스 LLM/오버레이 STT(Phase 2~5)는 실기기+실모델 없이는 끝까지 검증할 수 없는
경계까지 구조를 잡아두었다 — 무엇이 진짜로 동작 검증됐고 무엇이 남았는지는 "알려진 제약" 절에
정리했다.

## 빌드

```
./gradlew assembleDebug
./gradlew testDebugUnitTest   # PromptBuilder/ResponseParser/Purpose 등 순수 로직 단위 테스트
```

Android Studio(최신 Hedgehog 이상)로 루트 디렉터리를 열면 그대로 인식된다. `minSdk 26 / compileSdk 34`.

> 이 리포지토리를 만든 샌드박스 환경은 Android SDK와 `dl.google.com`/Gradle 배포 서버 접근이
> 막혀 있어 실제 `./gradlew build`를 이 안에서 끝까지 실행해 검증하지 못했다. 코드는 문법과 API
> 시그니처를 수동으로 재확인했지만, **Android Studio에서 최초 동기화/빌드를 한 번 돌려보는 것을
> 권장한다.**

## 패키지 구조

```
data/model/          도메인 모델 — Project, Purpose, DocType, GlossaryTerm, MemoStatus
data/local/room/     Room: ProjectIndexEntity(카드 목록 캐시), MemoEntity(메모 이력), DAO
data/local/config/   project.json 읽기/쓰기(ProjectConfigStore), 신규 프로젝트 폴더/파일 생성
data/repository/     ProjectRepository, MemoRepository — UI가 실제로 호출하는 창구

document/            StorageManager(SAF I/O), DocumentManager(Phase 4/5 파이프라인 실행),
                      PurposeDocumentTemplates(기본 md 스켈레톤), MemoNotifier

llm/                 LlmEngine 인터페이스, GemmaLlmEngine(MediaPipe tasks-genai 구현),
                      LlmSessionManager(지연 로딩 + 세션 타임아웃)
llm/download/        ModelDownloadManager/Worker — WorkManager+OkHttp, Range 이어받기, 체크섬

prompt/               PromptBuilder, ResponseParser(JSON 파싱+재시도), IntentClassifier,
                      GlossaryScanner, PurposePromptTemplates(목적별 지시문)

overlay/              OverlayService(항상 떠있는 녹음 버튼, Foreground Service), 권한 헬퍼
stt/                  SpeechToTextManager — 프레임워크 SpeechRecognizer 래퍼(온라인 전용),
                      블루투스 헤드셋 연결 시 SCO 오디오 링크 자동 개설/해제

ui/                   Jetpack Compose 화면 — projectlist/projectdetail/projectsettings/newproject
di/AppContainer.kt     수동 서비스 로케이터(Hilt 미사용) — Application에서 한 번 생성
```

## 핵심 데이터 흐름

1. `OverlayService`의 녹음 버튼 탭 → `SpeechToTextManager`(온라인 STT) → 텍스트 획득
2. `DocumentManager.processVoiceMemo(projectId, text)`
   - `MemoRepository.recordPending` — 실패해도 원문이 남도록 즉시 PENDING 기록
   - `IntentClassifier` — "문서 반영" vs "용어집 추가" 1차 판단
   - 문서 반영이면 `PromptBuilder`가 목적(Purpose)별 지시문 + 용어집 힌트 + 기존 파일 전체 내용을
     담아 프롬프트 구성 → `LlmSessionManager`(Gemma) 호출 → `ResponseParser`가 JSON 파싱
     (실패 시 재생성 재시도, 최종 실패 시 원문을 `MemoEntity.failureReason`에 백업)
   - 성공하면 모델이 돌려준 파일 전체 내용으로 해당 `.md`를 덮어쓰고, Room 인덱스 미리보기 갱신,
     알림 표시
3. 진실의 원천은 항상 파일(`*.md`, `project.json`) — Room은 목록/이력 조회를 빠르게 하기 위한
   재구성 가능한 캐시일 뿐이다.

## 알려진 제약 / 다음에 확인할 것

- **실기기 미검증**: 오버레이 창(WindowManager), 마이크 권한 플로우, MediaPipe `tasks-genai` 추론은
  에뮬레이터/실기기에서 아직 돌려보지 않았다. 특히 `GemmaLlmEngine`이 사용하는
  `LlmInference`/`LlmInferenceOptions` API 시그니처는 공개 문서 기준으로 작성했으니 실제
  라이브러리 버전과 맞는지 한 번 빌드해서 확인 필요.
- **모델 다운로드 URL 미검증** (`llm/download/ModelDownloadManager.kt`): `todo.md` #52-53과 동일한
  이슈로, Hugging Face 게이트/로그인 필요 여부와 정확한 파일명을 실제 다운로드로 확인해야 한다.
  `EXPECTED_SHA256`도 비어 있어 체크섬 검증이 아직 꺼져 있는 상태(값을 채우면 자동으로 켜짐).
  라이선스 동의 화면(Gemma Prohibited Use Policy)은 아직 UI로 구현하지 않았다.
- **문서 라우팅**: 별도의 규칙 기반 라우터 대신, LLM이 JSON 응답의 `targetFile` 필드로 스스로
  라우팅하도록 설계했다(모델이 문맥을 가장 잘 알기 때문). 오동작 시 규칙 기반 폴백을 추가할 수 있다.
  파일 병합도 LLM이 기존 파일 전체 + 새 발화를 받아 전체 파일을 다시 써서 돌려주는 방식이라, 별도
  diff 알고리즘은 없다.
  라이선스 동의 화면(Gemma Prohibited Use Policy 고지)은 아직 UI로 구현하지 않았다.
- **Undo/메모 전문 검색**(Phase 5-1 후반부), **RAM/발열 기반 E2B/E4B 자동 선택**(Phase 3-1 후반부),
  **용어집 자동 감지(반복 등장 미등록 용어)**는 이번 스캐폴딩 범위에 포함하지 않았다.
- 폰트는 목업의 IBM Plex Mono/Inter 대신 시스템 monospace/sans-serif로 대체했다 — 정확히 맞추려면
  `ui/theme/Type.kt`에 다운로드 가능한 폰트(Downloadable Fonts) 또는 번들 `.ttf`를 추가하면 된다.

## 설계 결정 (요약)

원본 `todo.md`의 "참고 결정 사항"과 동일하다: STT는 항상 온라인(오프라인 언어팩 미사용), LLM은
Gemma-4-E4B on-device(서버 없음), 저장은 SAF 기반 로컬 파일 + 수동 내보내기만 지원, 문서는 파일
시스템 Markdown이 진실의 원천이고 Room DB는 이력/인덱스 전용, 개인 전용 사이드로드 APK(Play 배포
아님).
