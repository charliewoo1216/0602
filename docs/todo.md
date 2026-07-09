# AI 프로젝트 메모 앱 - 개발 작업 리스트

> 목표: 음성으로 아이디어를 기록하면 온디바이스 LLM(Gemma-4-E4B)이 프로젝트 Markdown 문서를 자동 업데이트하는 개인용 Android 앱
> 구성: STT(온라인) + LLM(온디바이스, 완전 로컬) + 서버/동기화 없음

---

## Phase 1. 프로젝트 카드 목록 UI (Keep 스타일)

- [ ] 프로젝트 데이터 모델 정의 (name, color, path, pinned, lastUpdated, previewText, **purpose, glossary**)
- [ ] 프로젝트 목록 화면 - 2열 카드 그리드 레이아웃 (Jetpack Compose)
- [ ] 카드 색상 랜덤/수동 지정 기능
- [ ] 고정(Pin) 기능 - 상단 "고정됨" 섹션 분리
- [ ] 우측 하단 `+` FAB 버튼 - 새 프로젝트 생성 다이얼로그
- [ ] 프로젝트 생성 시 폴더/기본 md 파일 자동 생성 (목적에 따라 구조 다르게)
- [ ] 카드 탭 → 프로젝트 상세 화면 이동
- [ ] 상단 검색바 - 프로젝트명/내용 검색
- [ ] 카드 미리보기 텍스트 (최근 반영 내용 일부 표시)

### 1-1. 프로젝트별 설정 - 음성 (목적 & 용어집)
- [ ] 프로젝트 생성/설정 화면에 "목적(Purpose)" 선택 UI 추가
  - 코딩용 plan.md / 할일 계획(todo) / 회의록(meeting) / 아이디어 브레인스토밍 / 자유 형식
- [ ] 목적별 문서 구조 템플릿 정의 (예: 코딩용 → Phase/Task/의존관계/완료조건)
- [ ] 용어집(Glossary) 관리 UI - 태그 추가/삭제 (예: Kafka, MCP, JWT, RabbitMQ, LangGraph)
- [ ] 프로젝트 설정값(purpose, glossary) 로컬 저장 (project.json 등)
- [ ] 설정 변경 시 기존 문서 구조에 미치는 영향 처리 (마이그레이션 없이 이후 반영분부터 적용)

### 1-2. LLM 기반 용어집 보강
- [ ] "문서에서 용어 스캔하기" 버튼 - 기존 md 문서 전체를 LLM이 스캔해 기술 용어/고유명사 후보 추출
- [ ] 추천 용어를 "승인 대기" 칩으로 표시, 탭하면 확정 용어집으로 전환
- [ ] 오버레이 음성으로 직접 용어 추가 지원 (예: "용어집에 트랜스포머 아키텍처 추가해줘")
- [ ] 평소 메모 처리 중 반복 등장하는 미등록 용어 자동 감지 → 승인 대기 상태로 추천

## Phase 2. 오버레이 녹음 + STT (온라인)

- [ ] `SYSTEM_ALERT_WINDOW` 권한 요청 플로우
- [ ] `RECORD_AUDIO`, `INTERNET` 권한 요청 플로우
- [ ] OverlayService (Foreground Service) 구현
- [ ] WindowManager 기반 플로팅 버튼 뷰 생성
- [ ] 버튼 드래그 이동 기능
- [ ] 버튼 탭 → 녹음 시작/종료 토글, 색상으로 상태 표시
- [ ] `SpeechRecognizer` 연동 (기본 온라인 모드)
- [ ] 인식 결과 텍스트 콜백 처리
- [ ] 네트워크 없을 시 실패 안내 토스트 처리
- [ ] 현재 선택된 프로젝트(project_id) 오버레이 상태에 연결

## Phase 3. LlmEngine (온디바이스 Gemma-4-E4B)

- [ ] `com.google.mediapipe:tasks-genai` 의존성 추가

### 3-1. 모델 다운로드 모듈 (자체 다운로드 방식, B안)
- [ ] Hugging Face `litert-community/gemma-4-E4B-it-litert-lm` 다운로드 URL 확인
- [ ] HF 로그인/게이트 필요 여부 실제 다운로드 테스트로 확인
- [ ] `WorkManager` + `OkHttp` 기반 백그라운드 다운로드 구현
- [ ] Range 헤더 기반 이어받기(Resume) 처리
- [ ] 다운로드 진행률(%) UI 표시
- [ ] 다운로드 완료 후 파일 크기/체크섬 검증
- [ ] Wi-Fi 전용 다운로드 옵션 (모바일 데이터 소진 방지)
- [ ] 모델 파일 저장 경로 관리 (`getExternalFilesDir` - 앱 전용 저장공간)
- [ ] 최초 실행 시 "라이선스 동의" 화면 (Gemma Prohibited Use Policy 고지)
- [ ] (추후 Play 배포 대비) 기기 RAM/발열 상태에 따른 E2B/E4B 자동 선택 로직

### 3-2. 추론 세션 관리
- [ ] `LlmInference` 세션 생성/해제 로직 (Lazy Loading - 사용 시에만 로드)
- [ ] 모델 로딩 인디케이터 UI
- [ ] 메모리/배터리 고려한 세션 타임아웃 처리
- [ ] `generateResponse()` 호출 테스트 (단순 프롬프트로 검증)

## Phase 4. PromptBuilder / ResponseParser

- [ ] **의도 분류(Intent Classification) 1차 단계** - STT 텍스트가 "문서 반영"인지 "용어집 추가 명령"인지 먼저 판단
- [ ] 프로젝트 내 관련 문서 선별 로직 (전체 대신 관련 md만 선택)
- [ ] 프롬프트 템플릿 설계 (기존 문서 + 새 발화 + "JSON만 출력" 지시 + few-shot 예시)
- [ ] 문서 라우팅 로직 (requirements/todo/architecture/ideas/decisions/meeting 중 판단)
- [ ] 충돌 감지 규칙/프롬프트 반영
- [ ] JSON 파싱 및 실패 시 재시도 로직
- [ ] 파싱 실패 시 원문 로그 백업 처리

### 4-1. STT 오류 보정 / 목적별 템플릿
- [ ] "STT 결과라 오타/누락/오인식 가능성 있음, 문맥으로 의도 추론" 지시문 프롬프트에 고정 삽입
- [ ] 프로젝트의 용어집(glossary)을 프롬프트 힌트로 삽입 (STT 오인식 기술 용어 보정용)
- [ ] 목적(purpose)별 시스템 프롬프트 템플릿 분기 처리
  - 코딩용 plan.md: Phase/Task 구조, 우선순위, 의존관계, 완료조건 명시 지시
  - 할일 계획: 체크박스 중심, 마감일 유무 판단
  - 회의록: 일시/참석자/논의사항/액션아이템 구조화
  - 아이디어: 자유 서술 + 카테고리 태그
  - 자유 형식: 구조 강제 없이 LLM 판단에 위임
- [ ] 용어집 보정 결과와 원본 STT 텍스트를 함께 로그로 남겨 추후 검수 가능하게 처리

## Phase 5. DocumentManager / StorageManager

- [ ] SAF(Storage Access Framework) 기반 저장 위치 선택/권한 처리
- [ ] 프로젝트별 폴더 구조 read/write 유틸
- [ ] md 파일 업데이트 반영 (중복 제거, 기존 내용 유지, 필요한 부분만 수정)
- [ ] 신규 문서 자동 생성 로직
- [ ] 처리 완료 알림(Notification) 연동
- [ ] 카드 미리보기 텍스트 갱신 트리거
- [ ] "내보내기"(공유 시트) 기능 - Drive/이메일 등으로 수동 전달

### 5-1. Room DB 기반 메모 이력 관리 (md 파일과 병행)
- [ ] Room 의존성 추가, `Memo` Entity 정의
  - id, project_id, timestamp, raw_stt_text, corrected_summary, target_file, status(PENDING/APPLIED/FAILED/CONFLICT), voice_file_path(선택)
- [ ] 메모 저장 시점: STT 완료 직후 status=PENDING으로 우선 기록
- [ ] LLM 처리 완료 후 status를 APPLIED/FAILED/CONFLICT로 업데이트
- [ ] "진실의 원천은 md 파일" 원칙 확정 - DB는 이력/인덱스 용도로만 사용, 불일치 시 파일 우선
- [ ] 파싱 실패(FAILED) 메모 재시도 UI/로직
- [ ] 메모 타임라인 조회 화면 (프로젝트 상세 내 "이력" 탭)
- [ ] Undo 기능 - 특정 메모 반영 이전 상태로 되돌리기 (파일 diff 기반)
- [ ] 메모 전문 검색 (DB 쿼리 기반, 파일 파싱보다 빠르게)
- [ ] 카드 미리보기용 "최근 메모 N개" 쿼리 연동

## Phase 6. 통합 테스트 / 마무리

- [ ] 전체 플로우 통합 테스트 (녹음 → STT → LLM → md 반영 → 알림)
- [ ] 비행기 모드/네트워크 없는 상태에서 LLM 파트만 정상 동작하는지 확인
- [ ] 다수 프로젝트 전환 시 상태 관리 점검
- [ ] 배터리/발열 체감 테스트 (E4B 모델 반복 호출)
- [ ] APK 디버그 빌드 및 개인 사이드로드 설치

---

## 참고 결정 사항 (Decisions Log)

- STT는 Android 기본 `SpeechRecognizer` **온라인 모드** 사용 (오프라인 언어팩 방식 채택 안 함)
- LLM은 **Gemma-4-E4B**, LiteRT-LM 기반 온디바이스 추론 (서버 없음)
- 저장은 **SAF 기반 로컬 저장** + 수동 내보내기만 지원 (자동 서버 동기화/Git 연동 없음)
- DB 없이 **파일시스템 기반 Markdown**으로만 관리 (Claude Code/Cursor 등에서 직접 열람 가능하도록)
- 개인 전용 APK (Play Store 배포 X, 사이드로드 설치)
- 앱의 핵심 가치는 STT 품질 자체가 아니라, **STT 오류를 LLM이 문맥/용어집으로 보정해 의도를 정확히 파악하는 것**
- 프로젝트별로 "목적(purpose)"과 "용어집(glossary)"을 설정 > 음성 메뉴에서 지정, 목적에 따라 문서 구조/프롬프트 템플릿 분기
- plan.md와 기존 7개 문서 체계의 최종 관계는 설계 진행하며 재검토 예정 (미확정)
- 저장은 **md 파일(진실의 원천) + Room DB(메모 이력/메타데이터) 병행 방식**으로 확정 - DB로 문서 자체를 대체하지 않음
