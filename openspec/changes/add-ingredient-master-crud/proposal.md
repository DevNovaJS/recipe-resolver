## Why

재료 추천, 보유 재고 관리, 요리 기록 등 모든 핵심 도메인의 기반이 되는 재료 사전(master)이 없으면 이후 기능을 구현할 수 없다. 재료 식별을 정수 ID 기반으로 통일하기 위해 가장 먼저 재료 마스터 CRUD API를 구축한다.

## What Changes

- `ingredient_category` 테이블 신규 생성 (카테고리 마스터, 시드 데이터 포함 / CRUD API는 이번 change 제외)
- `ingredient` 테이블 신규 생성 (id, name, default_unit, category_id, description)
  - `name` UNIQUE 제약
  - `category_id` → `ingredient_category.id` 논리적 참조 (FK 제약조건 없음)
  - `default_unit` Java enum (GRAM, MILLILITER, PIECE 등)
  - `description` nullable
- `GET /ingredients` — 페이징 + category 필터
- `GET /ingredients/{id}` — 단건 조회
- `POST /ingredients` — 등록
- `PUT /ingredients/{id}` — 수정
- `DELETE /ingredients/{id}` — Hard delete

## Capabilities

### New Capabilities

- `ingredient-master-crud`: 재료 사전 등록/조회/수정/삭제 API 및 DB 스키마

### Modified Capabilities

(없음)

## Impact

- **DB**: `ingredient_category`, `ingredient` 테이블 신규 (Flyway 마이그레이션)
- **API**: `/ingredients` 엔드포인트 신규
- **의존**: 이후 보유 재고, 레시피 추천, 요리 기록 등 모든 도메인이 `ingredient.id`를 FK로 참조
