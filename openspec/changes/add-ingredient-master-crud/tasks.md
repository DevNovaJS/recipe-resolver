## 1. 프로젝트 기반 설정

- [x] 1.1 build.gradle에 Flyway 의존성 추가 (`org.flywaydb:flyway-mysql`)
- [x] 1.2 application.yml에 Flyway 설정 추가 (`spring.flyway.enabled=true`, `locations=classpath:db/migration`)
- [x] 1.3 application.yml JPA ddl-auto를 `validate`로 설정

## 2. DB 스키마 (Flyway 마이그레이션)

- [x] 2.1 `init_ingredient.sql` 생성 — `ingredient_category` 테이블 DDL
- [x] 2.2 `init_ingredient.sql`에 `ingredient` 테이블 DDL 추가 (name UNIQUE, category_id BIGINT FK 제약 없음)
- [x] 2.3 `init_ingredient.sql`에 `ingredient_category` 시드 데이터 INSERT 추가 (채소, 육류, 해산물, 유제품, 양념/소스, 곡물, 기타 등)

## 3. 도메인 모델

- [x] 3.1 `IngredientCategory` Entity 작성 (`ingredient_category` 테이블 매핑)
- [x] 3.2 `UnitType` enum 작성 (GRAM, MILLILITER, PIECE, TABLESPOON, TEASPOON, CUP)
- [x] 3.3 `Ingredient` Entity 작성 (`ingredient` 테이블 매핑, `@Column(unique=true)` name, `@Enumerated(STRING)` defaultUnit)

## 4. Repository

- [x] 4.1 `IngredientCategoryRepository` 작성 (JpaRepository)
- [x] 4.2 `IngredientRepository` 작성 (JpaRepository + `findAllByCategoryId(Long, Pageable)`)

## 5. DTO

- [x] 5.1 `IngredientRequest` 작성 (name, categoryId, defaultUnit, description — Bean Validation 포함)
- [x] 5.2 `IngredientResponse` 작성 (id, name, categoryId, defaultUnit, description)

## 6. Service

- [x] 6.1 `IngredientService` 작성 — 등록 (categoryId 존재 검증 포함)
- [x] 6.2 `IngredientService`에 단건 조회 구현
- [x] 6.3 `IngredientService`에 목록 조회 구현 (categoryId 필터 + Pageable)
- [x] 6.4 `IngredientService`에 수정 구현
- [x] 6.5 `IngredientService`에 삭제(Hard delete) 구현

## 7. Controller

- [x] 7.1 `IngredientController` 작성 — POST /ingredients
- [x] 7.2 `IngredientController`에 GET /ingredients/{id} 추가
- [x] 7.3 `IngredientController`에 GET /ingredients 추가 (Pageable + categoryId 파라미터)
- [x] 7.4 `IngredientController`에 PUT /ingredients/{id} 추가
- [x] 7.5 `IngredientController`에 DELETE /ingredients/{id} 추가

## 8. 공통 에러 처리

- [x] 8.1 `GlobalExceptionHandler` 작성 (`@RestControllerAdvice`) — EntityNotFoundException → 404, DataIntegrityViolationException → 409, MethodArgumentNotValidException → 400

## 9. 검증

- [ ] 9.1 애플리케이션 기동 확인 (Flyway 마이그레이션 정상 실행, 테이블 생성 확인)
- [ ] 9.2 HTTP 클라이언트(Postman/curl)로 전체 CRUD 시나리오 수동 테스트

