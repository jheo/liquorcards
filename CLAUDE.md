# Liquor Cards

주류 정보 검색/컬렉션 관리 풀스택 앱. AI(Gemini)가 Google Search grounding으로 주류를 식별하고, 외부 DB에서 데이터를 보강한 뒤, 최종 종합 결과를 카드 형태로 제공한다.

## 기술 스택

- **Backend**: Kotlin + Spring Boot 3.2, JPA, SQLite (`data/liquir.db`)
- **Frontend**: React 19 + TypeScript, Vite, React Router, Lucide Icons
- **AI**: Gemini 2.5 Flash (Google Search grounding + JSON mode)
- **이미지**: Gemini Image Generation (`gemini-2.0-flash-exp`)
- **외부 데이터**: Open Library, Whisky Hunter, Whisky.com, Untappd, Open Food Facts

## 프로젝트 구조

```
liquor-cards/
├── server/                          # Spring Boot 백엔드
│   └── src/main/kotlin/com/liquir/
│       ├── controller/              # REST API 컨트롤러
│       │   ├── LiquorController.kt  # CRUD + AI lookup + SSE 스트리밍
│       │   ├── AiController.kt      # /api/ai/search 엔드포인트
│       │   └── ImageController.kt   # 이미지 서빙
│       ├── service/
│       │   ├── AiService.kt         # AI 인터페이스 + GoogleSearchResult
│       │   ├── AiServiceImpl.kt     # Gemini 2단계 파이프라인 구현
│       │   ├── ExternalDatabaseService.kt  # 외부 DB 수집
│       │   ├── ImageGenerationServiceImpl.kt
│       │   ├── LiquorService.kt     # CRUD 비즈니스 로직
│       │   └── WebCrawlerService.kt # 웹 크롤링 유틸
│       ├── dto/LiquorDtos.kt        # 모든 DTO 정의
│       └── model/Liquor.kt          # JPA 엔티티
├── frontend/src/
│   ├── api/client.ts                # API 클라이언트 + SSE 스트리밍
│   ├── pages/                       # 페이지 컴포넌트
│   ├── components/                  # UI 컴포넌트
│   ├── hooks/                       # 커스텀 훅 + SearchQueueContext
│   ├── i18n/                        # 다국어 (EN/KO)
│   └── types/liquor.ts              # 타입 정의
└── data/liquir.db                   # SQLite DB 파일
```

## AI 검색 파이프라인

```
사용자 입력 (예: "obc 코스모스 에일")
  │
  ▼
Step 1: searchWithGoogle()
  ├─ Phase 1: callGeminiWithSearch() → Google Search grounding → 텍스트 수집
  └─ Phase 2: callGemini() → JSON mode로 텍스트를 구조화된 JSON 변환
  → 결과: canonicalName, category, searchQueries, data[], imageUrls[]
  │
  ▼
Step 2: externalDatabaseService.collectAll()
  → 외부 DB에서 추가 데이터 보강
  │
  ▼
Step 3 (병렬):
  ├─ synthesizeData() → 모든 데이터 종합 (Gemini JSON mode)
  └─ generateImage() → 이미지 생성/선택
  │
  ▼
최종 AiLookupResponse
```

**핵심 제약**: Google Search grounding과 `responseMimeType: "application/json"`은 호환 불가. 그래서 Phase 1은 텍스트 수집, Phase 2에서 JSON 변환하는 2단계 구조를 사용한다.

## 개발 환경

```bash
# 서버 (포트 8080)
cd server && ./gradlew bootRun

# 프론트엔드 (포트 5173)
cd frontend && npm run dev
```

## 빌드 & 테스트

```bash
# 서버 컴파일 확인
cd server && ./gradlew compileKotlin

# 서버 테스트
cd server && ./gradlew test

# 프론트엔드 타입 체크
cd frontend && npx tsc --noEmit

# 프론트엔드 테스트
cd frontend && npm run test
```

## 환경 변수

`server/.env`에 설정:

```
GOOGLE_API_KEY=...          # 필수: Gemini API + Google Search grounding + 이미지 생성
UNTAPPD_CLIENT_ID=...       # 선택: 맥주 데이터 보강
UNTAPPD_CLIENT_SECRET=...   # 선택: 맥주 데이터 보강
```

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/liquors` | 전체 조회 (category, status, search, sort 필터) |
| GET | `/api/liquors/{id}` | 단건 조회 |
| POST | `/api/liquors` | 생성 |
| PUT | `/api/liquors/{id}` | 수정 |
| DELETE | `/api/liquors/{id}` | 삭제 |
| POST | `/api/liquors/ai-lookup` | AI 검색 (동기) |
| POST | `/api/liquors/ai-lookup-stream` | AI 검색 (SSE 스트리밍) |
| GET | `/api/liquors/search-result/{id}` | 캐시된 검색 결과 조회 |
| POST | `/api/ai/search` | AI 검색 (AiController 경유) |

## 코딩 컨벤션

- 커밋 메시지는 한국어로 작성, `feat:` / `fix:` / `refactor:` 접두사 사용
- Kotlin: Spring 스타일, data class 활용, `@Value`로 설정 주입
- Frontend: 함수형 컴포넌트, Context API로 상태 관리, i18n은 EN/KO 이중 지원
- JSON 필드: camelCase (Kotlin) ↔ camelCase (Frontend), `@JsonAlias`로 snake_case 호환
