## Context

Spring Boot 4.0.5 + Java 25 기반 신규 프로젝트. 현재는 애플리케이션 메인 클래스만 존재하며 모든 것을 새로 구축해야 한다. MySQL(JPA), Elasticsearch, Redis가 이미 의존성으로 선언되어 있고, LLM API 연동(RestClient), Spring Batch, Resilience4j를 추가로 도입한다.

## Goals / Non-Goals

**Goals:**
- 재료 → 레시피 매칭을 `ingredient.id` 정수 FK 조인으로 빠르고 정확하게 수행
- 재료 사전(`ingredient`)과 사용자 보유 재고(`user_ingredient`)를 분리하여 오타·중복·동의어 문제를 입력 단계에서 봉쇄 (자동완성으로 사전 선택만 허용)
- 레시피 필수재료(`is_essential`) 기준으로 커버율을 계산하여 추천 품질 확보
- Elasticsearch를 활용한 빠른 전문 검색 및 자동완성
- 요리 완료 시 재료 차감의 트랜잭션 안정성 보장
- LLM API 장애 시에도 DB 기반 추천은 정상 동작 (서킷브레이커 격리)
- Testcontainers 기반 테스트로 실제 인프라와 동일한 환경에서 검증
- 향후 AI 기반 시맨틱 검색 확장을 위한 데이터 스키마(`recipe.description`, `RecipeDocument.description`) 준비

**Non-Goals:**
- 사용자 인증/인가 (현 단계에서는 단일 사용자 기준)
- 프론트엔드 UI 구현
- 레시피 데이터 크롤링/수집 파이프라인 (초기 데이터는 수동 등록 또는 시드)
- 실시간 푸시 알림 (유통기한 임박은 조회 API로 제공)
- 수량/단위 변환 (예: "100g" vs "3개") — 초기에는 "있다/없다"만 매칭, 차감은 동일 단위 가정
- 대체재료/동의어 사전 (예: 진간장 ↔ 양조간장) — 초기에는 사전(master)에 대표명만
- 재료 계층 구조 (예: 채소 > 잎채소 > 배추) — 카테고리는 flat ENUM
- **AI 임베딩 기반 시맨틱 검색 및 대체재 제안 (본 change에서는 스키마만 준비, 구현은 후속 change로 분리)**

## Decisions

### 1. 패키지 구조: 도메인 기반 패키지

```
com.custom.recipe
├── ingredient/          # 재료 사전 + 사용자 보유 재고
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   └── dto/
├── recipe/              # 레시피 & 추천
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   └── dto/
├── search/              # ES 검색
│   ├── controller/
│   ├── service/
│   ├── document/
│   └── repository/
├── cooking/             # 요리 기록 & 통계
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/
│   ├── dto/
│   └── batch/
├── llm/                 # LLM 연동
│   ├── service/
│   ├── dto/
│   └── config/
└── global/              # 공통 (예외 처리, 설정)
    ├── exception/
    └── config/
```

**이유**: 기능 단위로 응집도가 높아지고, 각 도메인의 책임이 명확해진다. 레이어 기반(controller/, service/, repository/ 최상위) 대비 도메인 간 의존 관계를 파악하기 쉽다.

### 2. 재료 매칭 모델: 사전(master) + 보유 재고 분리

재료를 "개념/사전"과 "실제 보유 재고"로 분리하고, 레시피 요구 재료도 사전을 참조한다. 모든 매칭은 `ingredient.id` 정수 FK 조인으로 수행한다.

```
ingredient (재료 사전 — "토마토"라는 개념 그 자체)
  id PK, name UNIQUE, category, default_unit

user_ingredient (사용자 냉장고 재고)
  id PK, ingredient_id FK, quantity, unit, expiration_date, purchased_at
  INDEX (expiration_date), INDEX (ingredient_id)

recipe
  id PK, name, description, cooking_time, difficulty, steps

recipe_ingredient (레시피가 요구하는 재료)
  recipe_id FK, ingredient_id FK,
  required_amount, required_unit,
  is_essential BOOLEAN DEFAULT true,
  PK (recipe_id, ingredient_id)
  INDEX (ingredient_id)
```

**이유**:
- 정수 FK 조인으로 커버율 계산이 인덱스 최적 경로로 수행됨
- 사용자 입력은 자동완성으로 사전 선택만 허용 → 오타·중복·동의어 문제 원천 차단
- 재료 메타데이터(영양, 가격 등) 확장 시 `ingredient` 한 곳만 수정

**대안 검토**:
- 단일 `ingredient` 테이블에 사용자 보유 재고까지 넣기 → 레시피도 동일 row를 참조해야 하므로 의미가 꼬임
- 이름 문자열 매칭 → 오타/변형에 취약, JOIN 성능 저하

### 3. 레시피 추천 매칭: DB 쿼리 기반 커버율 계산

레시피별 필수재료 수 대비 사용자 보유 재료 수의 비율을 DB 쿼리로 계산한다. `is_essential = true`인 재료만 커버율 대상이며, 선택재료는 응답에 "있으면 좋은 재료"로 별도 표시한다.

```sql
SELECT r.id, r.name,
       COUNT(ri.ingredient_id)                              AS total_essential,
       COUNT(ui.ingredient_id)                              AS matched,
       COUNT(ri.ingredient_id) - COUNT(ui.ingredient_id)    AS missing_count
FROM recipe r
JOIN recipe_ingredient ri ON r.id = ri.recipe_id AND ri.is_essential = true
LEFT JOIN user_ingredient ui ON ri.ingredient_id = ui.ingredient_id
GROUP BY r.id
HAVING missing_count <= :maxMissing
ORDER BY missing_count ASC, matched DESC
```

**이유**: ES의 검색 특화 기능보다는 관계형 JOIN이 커버율 계산에 자연스럽다. 레시피 수가 수만 건 이하인 초기 규모에서 성능 문제 없으며, 필요 시 인덱스 튜닝으로 대응 가능. 선택재료를 커버율에서 제외하면 "두부 없다고 김치찌개가 거의매칭으로 밀려나는" 품질 저하를 방지한다.

**대안 검토**: ES로 매칭 → 검색에는 강하나 비율 계산 집계가 복잡하고, RDB 데이터와 실시간 동기화 필요. 애플리케이션 메모리 계산 → 레시피 수 증가 시 메모리 부담.

### 4. RDB → ES 동기화: @TransactionalEventListener 기반 이벤트

레시피 생성/수정/삭제 시 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 ES 동기화 이벤트를 발행한다.

```
Recipe 저장 (JPA) → 트랜잭션 커밋 → RecipeChangedEvent 발행 → ES 인덱싱
```

`RecipeDocument`는 `id, name, description, ingredients[{id, name, essential}], cookingTime, difficulty` 등을 평탄화하여 저장한다. `description` 필드는 현 단계에서 키워드 검색 대상이며, 후속 change에서 동일 필드에 대한 임베딩 벡터를 추가할 예정이다.

**이유**: 트랜잭션 커밋 후에만 이벤트가 발생하므로 RDB-ES 간 불일치 최소화. CDC(Debezium) 대비 인프라 복잡도가 낮고, 스케줄링 대비 지연이 적다.

**대안 검토**: Debezium CDC → 가장 견고하나 Kafka + Debezium 인프라 추가 필요. 스케줄링 → 구현 단순하나 동기화 지연(분 단위) 발생.

**실패 대응**: ES 인덱싱 실패 시 로그 기록 + 재시도 큐(Spring Retry). 최악의 경우 수동 전체 재인덱싱 배치 보유.

### 5. LLM 캐싱: 재료 ID 정렬 해시

사용자 보유 재료 ID 목록을 정렬한 후 SHA-256 해시를 Redis 캐시 키로 사용한다.

```
캐시 키: "llm:recipe:{SHA256(sorted ingredient IDs)}"
TTL: 24시간
```

**이유**: 재료 순서에 무관하게 동일 조합을 같은 키로 매핑. 수량은 캐시 키에 포함하지 않는다 — LLM 추천은 "어떤 재료가 있는지"가 핵심이고, 수량 변동마다 캐시 미스가 발생하면 캐시 효율이 급락한다.

### 6. Resilience4j 적용 범위

| 패턴 | 대상 | 설정 |
|------|------|------|
| CircuitBreaker | LLM API 호출 | failureRateThreshold=50%, waitDuration=30s, slidingWindow=10 |
| RateLimiter | LLM API 호출 | limitForPeriod=10, limitRefreshPeriod=1m |
| TimeLimiter | LLM API 호출 | timeoutDuration=10s |
| Retry | ES 동기화 | maxAttempts=3, waitDuration=1s |

**Fallback**: 서킷 오픈 시 DB 기반 추천 결과만 반환 + "LLM 추천 일시 불가" 메시지.

### 7. 통계 배치: Spring Batch Job

월별 통계 집계를 Spring Batch Job으로 처리한다.

- **Job**: `cookingStatisticsJob` — 일 단위 스케줄링
- **Step 1**: 요리 횟수 집계
- **Step 2**: 자주 쓴 재료 TOP5 집계
- **Step 3**: 자주 해먹은 메뉴 TOP5 집계
- 집계 결과를 `cooking_statistics` 테이블에 저장

**이유**: 통계 쿼리가 실시간 API에 부하를 주지 않도록 분리. 배치 실패 시 재실행 가능한 멱등성 보장.

### 8. 글로벌 예외 처리 구조

```
@RestControllerAdvice
├── BusinessException (400) — 재료 부족, 유효하지 않은 입력 등
├── ResourceNotFoundException (404) — 재료/레시피/기록 미존재
├── ExternalServiceException (502) — LLM API 장애
├── CircuitBreakerOpenException (503) — 서킷 오픈 상태
└── Exception (500) — 예상치 못한 오류
```

응답 형식 통일:
```json
{
  "code": "INGREDIENT_NOT_FOUND",
  "message": "재료를 찾을 수 없습니다.",
  "timestamp": "2026-04-13T12:00:00"
}
```

### 9. AI 활용 전략 (향후 확장 — 본 change 범위 밖)

본 change에서는 **LLM 기반 창의적 레시피 추천**만 구현한다. 임베딩 기반 AI 기능은 데이터 스키마만 준비해두고 후속 change로 분리한다.

**향후 도입 예정 (Deferred)**:

| 기능 | 저장 위치 | 용도 |
|------|----------|------|
| 시맨틱 레시피 검색 | ES `RecipeDocument.description_vector` (dense_vector) | "매콤한 국물요리" 같은 자연어 검색 |
| 재료 대체재 제안 | ES `IngredientDocument.name_vector` (추후 도입 시) | "진간장 대신 양조간장" 제안 |
| 임베딩 자동완성 확장 | ES `IngredientDocument.name_vector` (추후 도입 시) | 오타/의미 기반 자동완성 |

**3단 매칭 아키텍처 (향후)**:
1. RDB 정확 매칭 (`ingredient.id` JOIN) — 현 구현
2. 임베딩 유사도 매칭 — 대체재 제안용 (후속 change)
3. LLM 생성 — 창의적 레시피 (현 구현)

**본 change에서의 데이터 준비**:
- `recipe.description` 컬럼 포함 (MySQL)
- `RecipeDocument.description` 필드 매핑 (Elasticsearch, text 타입)
- 후속 change에서 `RecipeDocument`에 `description_vector` (dense_vector, HNSW) 필드만 추가하면 바로 임베딩 파이프라인 연결 가능

**이유**: 지금 임베딩을 도입하면 모델 선정·파이프라인·운영 부담이 붙어 코어 기능 완성이 늦어진다. 반면 `description` 필드 준비만 해두면 후속 change에서 스키마 마이그레이션 없이 확장 가능하다.

## Risks / Trade-offs

- **[ES 동기화 지연]** → 이벤트 기반이므로 극히 짧은 지연은 존재. 실시간 정합성이 절대적이지 않은 검색 영역이므로 수용 가능. 재시도 + 전체 재인덱싱 배치로 보완.
- **[LLM 비용]** → Rate Limiter로 분당 호출 수 제한 + Redis 캐싱으로 중복 호출 최소화. 캐시 TTL 조정으로 비용/신선도 균형.
- **[단일 사용자 가정]** → 현 단계에서 인증 없이 구축. 추후 멀티유저 전환 시 재료/기록 테이블에 user_id FK 추가 필요. 초기부터 user_id 컬럼은 예약해두되 기본값 사용.
- **[Spring Batch 오버헤드]** → 초기 데이터 규모에서는 과할 수 있으나, 통계 집계의 멱등성과 재실행 보장이 장기적으로 가치 있음. 초기에는 단순 Step 구성으로 시작.
- **[Testcontainers 빌드 시간]** → MySQL + ES + Redis 컨테이너 동시 기동 시 테스트 시작까지 시간 소요. @SharedContainerLifecycle 활용으로 테스트 간 컨테이너 재사용.
- **[재료 사전 시드 데이터 커버리지]** → 초기 시드에 없는 재료는 레시피 등록/자동완성 누락 발생. 한국 가정식 기준 500개 수준으로 시작하고, 운영 중 "신규 재료 제안" 수집 로그로 점진 보완.
- **[단위 매칭 단순화]** → "100g vs 3개" 불일치 시 차감 API가 400 에러 반환. UX 혼란 가능하나, 초기 복잡도 제어를 위해 수용. 향후 재료별 단위 변환 테이블 도입 여지.
