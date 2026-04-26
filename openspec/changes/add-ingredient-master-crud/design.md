## Context

Spring Boot 4.0.5 + Java 25 + JPA(MySQL) 환경. 현재 도메인 코드 없음 — 재료 마스터가 모든 도메인의 기반이므로 가장 먼저 구축한다. Flyway 의존성은 현재 build.gradle에 없어 이번 change에서 추가 여부를 결정해야 한다.

## Goals / Non-Goals

**Goals:**
- `ingredient_category`, `ingredient` 테이블 스키마 확정 및 생성
- `/ingredients` CRUD REST API 구현
- category 시드 데이터 구성

**Non-Goals:**
- category CRUD API (다음 change)
- 자동완성 API (별도 change)
- 단위 변환 로직
- 인증/인가

## Decisions

**패키지 구조: 도메인 중심**
- `com.custom.recipe.ingredient` — Controller, Service, Repository, domain(Entity, Enum, VO), dto
- 도메인별 패키지 분리로 이후 확장 용이

**DB 스키마 관리: Flyway 도입**
- JPA `ddl-auto=validate` + Flyway 마이그레이션으로 스키마 버전 관리
- 대안(ddl-auto=create): 운영 환경 위험, 히스토리 추적 불가
- `init_ingredient.sql` 하나로 두 테이블 생성 + 시드 데이터 삽입

**default_unit: Java enum + DB STRING 저장**
- `@Enumerated(EnumType.STRING)` — DB에 "GRAM", "MILLILITER" 등 문자열로 저장
- 가독성 및 enum 순서 변경에 안전

**category_id: 논리적 참조만**
- FK 제약조건 없이 `BIGINT category_id` 컬럼으로 저장
- category CRUD API가 완성된 이후 FK 제약 추가 여부를 별도 change에서 결정

**삭제: Hard delete**
- `DELETE /ingredients/{id}` — DB row 직접 삭제
- 재고/요리기록과 연결이 생기기 전 단계이므로 무결성 이슈 없음

**에러 처리: GlobalExceptionHandler**
- `@RestControllerAdvice` 하나로 공통 에러 응답 포맷 통일
- `EntityNotFoundException` → 404, `DataIntegrityViolationException` → 409(name 중복)

**목록 조회: Spring Pageable + category 필터**
- `GET /ingredients?categoryId=1&page=0&size=20&sort=name,asc`
- `Page<IngredientResponse>` 응답

## Risks / Trade-offs

- **FK 없는 category_id** → 잘못된 categoryId로 등록 시 DB 레벨 차단 불가 → 애플리케이션 레벨 검증으로 보완 (category 테이블 존재 여부 확인)
- **Hard delete 시 이후 참조 문제** → 재고/요리기록 연결 전이므로 현재는 안전, 참조가 생기면 삭제 정책 재검토
