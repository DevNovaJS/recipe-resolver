## 1. 프로젝트 기반 설정

- [ ] 1.1 build.gradle에 Spring Batch, Resilience4j 의존성 추가
- [ ] 1.2 도메인 기반 패키지 구조 생성 (ingredient, recipe, search, cooking, llm, global)
- [ ] 1.3 application.yml 설정 (MySQL, Elasticsearch, Redis 연결 정보, Resilience4j 설정)
- [ ] 1.4 글로벌 예외 처리 구현 (@RestControllerAdvice, 공통 에러 응답 형식)
- [ ] 1.5 Testcontainers 기반 테스트 인프라 구성 (MySQL, ES, Redis 공유 컨테이너)

## 2. 재료 관리 (ingredient-management)

- [ ] 2.1 Ingredient(재료 사전) 엔티티 및 Category enum 정의 (id, name UNIQUE, category, default_unit)
- [ ] 2.2 UserIngredient(사용자 보유 재고) 엔티티 정의 (ingredient_id FK, quantity, unit, expiration_date, purchased_at)
- [ ] 2.3 IngredientRepository 구현 (이름 기반 조회, 자동완성 prefix 조회)
- [ ] 2.4 UserIngredientRepository 구현 (JPA, 유통기한 임박 조회 쿼리 포함)
- [ ] 2.5 IngredientService 구현 (재료 사전 조회, 자동완성)
- [ ] 2.6 UserIngredientService 구현 (등록, 수정, 삭제, 중복 합산 로직 — 동일 ingredient_id + 동일 단위 기준)
- [ ] 2.7 유통기한 임박 재료 조회 로직 구현 (D-N 기준, 경과 재료 포함 플래그)
- [ ] 2.8 IngredientController 구현 (재료 사전 자동완성 API)
- [ ] 2.9 UserIngredientController 구현 (CRUD + 목록 조회 + 유통기한 임박 조회)
- [ ] 2.10 재료 관리 DTO 정의 (사전 자동완성 응답, 사용자 재료 요청/응답)
- [ ] 2.11 재료 사전 초기 시드 데이터 준비 (한국 가정식 기준, 카테고리별 분류)
- [ ] 2.12 재료 관리 단위 테스트 (Service 레이어)
- [ ] 2.13 재료 관리 슬라이스 테스트 (@WebMvcTest, @DataJpaTest)

