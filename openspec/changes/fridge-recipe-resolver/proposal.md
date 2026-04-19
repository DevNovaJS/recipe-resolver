## Why

냉장고에 있는 재료를 효율적으로 활용하지 못해 음식물 낭비가 발생하고, 매번 "뭐 해먹지?"를 고민하는 문제를 해결한다. 사용자가 보유 재료를 등록하면 만들 수 있는 요리를 자동으로 추천해주고, 요리 기록과 재료 소진을 연동하여 냉장고 상태를 실시간으로 관리할 수 있는 플랫폼을 구축한다.

## What Changes

- 재료 도메인을 **`ingredient`(재료 사전) / `user_ingredient`(사용자 보유 재고)** 두 테이블로 분리하고, 모든 매칭은 `ingredient.id` 정수 FK 조인으로 수행
- 재료 사전 자동완성 API 및 사용자 보유 재료 CRUD 신규 구축 (수량, 단위, 유통기한 관리)
- 유통기한 임박 재료 조회 기능 (D-3, D-1, 당일 기준, 경과 재료 포함 표시)
- 레시피-재료 매핑 기반 메뉴 추천 엔진 구축 (완벽 매칭 / 거의 매칭, 필수재료(`is_essential`) 기준 커버율 계산)
- LLM API 연동을 통한 창의적 레시피 추천 + Redis 캐싱
- Elasticsearch 기반 레시피 전문 검색 (레시피명, 재료명, 복합 조건, 자동완성)
- RDB → Elasticsearch 데이터 동기화 (이벤트 기반, `@TransactionalEventListener(AFTER_COMMIT)`)
- 레시피 스키마에 `description` 필드 포함 및 `RecipeDocument`에도 매핑 — 향후 시맨틱 검색(임베딩) 확장 대비 데이터 준비
- 요리 기록 저장 및 사용 재료 수량 자동 차감 (트랜잭션)
- 요리 통계 API (이번 달 요리 횟수, 자주 쓴 재료 TOP5, 자주 해먹은 메뉴 TOP5)
- 통계 데이터 배치 집계 (Spring Batch)
- 글로벌 예외 처리 (@RestControllerAdvice)
- Resilience4j 기반 LLM API 서킷브레이커 및 Rate Limiting

## Capabilities

### New Capabilities

- `ingredient-management`: 재료 사전(master) 관리 + 사용자 보유 재료 등록/수정/삭제, 카테고리 분류, 자동완성, 유통기한 임박 조회
- `recipe-recommendation`: 보유 재료 기반 레시피 매칭 (필수재료 기준 완벽/거의 매칭), LLM 창의적 추천, Redis 캐싱
- `recipe-search`: Elasticsearch 기반 레시피 전문 검색, 복합 조건 검색, 자동완성, RDB-ES 동기화 (description 필드 포함)
- `cooking-log`: 요리 기록 저장, 재료 수량 자동 차감, 요리 통계 및 배치 집계

### Modified Capabilities

(없음 - 신규 프로젝트)

## Impact

- **신규 API**: 재료 사전 자동완성, 사용자 보유 재료 관리, 레시피 추천, 레시피 검색, 요리 기록, 통계 엔드포인트
- **데이터베이스**: MySQL 스키마 신규 생성
  - `ingredient` (재료 사전 — 시드 데이터 투입)
  - `user_ingredient` (사용자 보유 재고)
  - `recipe`, `recipe_ingredient` (`is_essential` 컬럼 포함)
  - `cooking_log`, `cooking_log_detail`, `cooking_statistics`
- **외부 인프라**: Elasticsearch 클러스터, Redis 인스턴스, LLM API 연동 필요
- **의존성 추가**: Spring Batch, Resilience4j (현재 build.gradle에 미포함)
- **테스트 인프라**: Testcontainers (MySQL, Elasticsearch, Redis) 구성 필요
- **향후 확장 대비**: `recipe.description`, `RecipeDocument.description` 필드는 현 단계에서 구현하되, 임베딩 벡터 필드와 AI 기반 시맨틱 검색은 본 change에서는 구현하지 않고 후속 change로 분리
