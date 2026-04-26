# RecipeResolver

냉장고에 있는 재료를 등록하면 만들 수 있는 요리를 자동으로 추천해주고, 요리 기록과 재료 소진을 연동하여 냉장고 상태를 실시간으로 관리하는 플랫폼입니다.

## 개요

"뭐 해먹지?" 고민과 음식물 낭비 문제를 해결하기 위해 만든 서비스입니다. 사용자가 보유한 재료를 기반으로 만들 수 있는 레시피를 매칭하고, LLM을 활용한 창의적 추천까지 제공합니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.5 |
| Database | MySQL (JPA/Hibernate) |
| Search | Elasticsearch |
| Cache | Redis |
| Batch | Spring Batch |
| Resilience | Resilience4j (CircuitBreaker, RateLimiter, TimeLimiter, Retry) |
| LLM | LLM API (RestClient) |
| Test | Testcontainers (MySQL, Elasticsearch, Redis) |

## 핵심 도메인

### 1. 재료 관리 (ingredient-management)

재료를 **재료 사전(master)** 과 **사용자 보유 재고** 두 계층으로 분리하여 관리합니다.

- 재료 사전(Ingredient): 한국 가정식 기준 재료 시드 데이터, 카테고리(flat ENUM) 분류
- 사용자 보유 재고(UserIngredient): 수량·단위·유통기한 관리
- 재료 자동완성: 사용자는 자유 텍스트 입력 불가, 사전에서 선택만 허용 → 오타·중복 원천 차단
- 유통기한 임박 조회: D-3, D-1, 당일 기준 선택, 경과 재료도 플래그로 포함
- 중복 등록 시 동일 재료+단위 기준으로 수량 합산

### 2. 레시피 추천 (recipe-recommendation)

보유 재료 기반으로 만들 수 있는 레시피를 매칭합니다.

- **필수재료(`is_essential`) 기준** 커버율 계산 → 완벽 매칭 / 거의 매칭 분류
- 선택재료는 "있으면 좋은 재료"로 별도 표시 (커버율 계산 제외)
- 매칭은 `ingredient.id` 정수 FK JOIN으로 수행 (문자열 매칭 아님)
- LLM API 연동을 통한 창의적 레시피 추천
  - 보유 재료 ID 정렬 → SHA-256 해시를 Redis 캐시 키로 사용 (TTL 24h)
  - Resilience4j CircuitBreaker / RateLimiter / TimeLimiter 적용
  - 서킷 오픈 시 DB 기반 추천 결과만 반환 (Fallback)

### 3. 레시피 검색 (recipe-search)

Elasticsearch를 활용한 빠른 전문 검색을 제공합니다.

- 레시피명·재료명 전문 검색, 복합 조건 검색, 자동완성
- RDB → ES 동기화: `@TransactionalEventListener(AFTER_COMMIT)` 이벤트 기반
- `description` 필드는 현 단계에서 키워드 검색 대상, 향후 임베딩 시맨틱 검색 확장 대비 스키마 준비
- ES 동기화 실패 시 Spring Retry(3회) + 전체 재인덱싱 배치로 보완

### 4. 요리 기록 & 통계 (cooking-log)

요리 완료 후 재료 소진까지 하나의 흐름으로 연결합니다.

- 요리 기록 저장 + 사용 재료 수량 자동 차감 (단일 트랜잭션)
- 통계 API: 이번 달 요리 횟수, 자주 쓴 재료 TOP5, 자주 해먹은 메뉴 TOP5
- Spring Batch로 통계 집계 배치 처리 (일 단위 스케줄링, 멱등성 보장)

## 패키지 구조

```
com.custom.recipe
├── ingredient/      # 재료 사전 + 사용자 보유 재고
├── recipe/          # 레시피 & 추천
├── search/          # Elasticsearch 검색
├── cooking/         # 요리 기록 & 통계 & 배치
├── llm/             # LLM API 연동
└── global/          # 공통 예외 처리, 설정
```

## DB 스키마 주요 테이블

```
ingredient           # 재료 사전 (name UNIQUE, category, default_unit)
user_ingredient      # 사용자 보유 재고 (ingredient_id FK, quantity, unit, expiration_date)
recipe               # 레시피 (name, description, cooking_time, difficulty, steps)
recipe_ingredient    # 레시피-재료 매핑 (is_essential, required_amount, required_unit)
cooking_log          # 요리 기록
cooking_log_detail   # 요리 기록 재료 상세
cooking_statistics   # 배치 집계 통계
```

## 설계 원칙

- **단일 사용자 가정**: 현 단계에서 인증 없이 구축. 추후 멀티유저 전환 시 user_id FK 추가 예정
- **단위 변환 없음**: 초기에는 동일 단위 가정, 수량 불일치 시 400 에러
- **대체재/동의어 없음**: 재료 사전에 대표명만 등록, 향후 확장 여지 있음
- **AI 임베딩 검색**: 본 단계에서는 `description` 스키마만 준비, 구현은 후속 단계로 분리

## 예외 처리

모든 API는 통일된 에러 응답 형식을 반환합니다.

```json
{
  "code": "INGREDIENT_NOT_FOUND",
  "message": "재료를 찾을 수 없습니다.",
  "timestamp": "2026-04-27T12:00:00"
}
```

| 상황 | HTTP 상태 |
|------|----------|
| 유효하지 않은 입력 | 400 |
| 리소스 미존재 | 404 |
| LLM API 장애 | 502 |
| 서킷브레이커 오픈 | 503 |
| 예상치 못한 오류 | 500 |
