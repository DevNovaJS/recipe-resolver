## ADDED Requirements

### Requirement: 재료 등록
시스템은 name, categoryId, defaultUnit, description(선택)을 받아 재료를 등록해야 한다.
- name은 전체에서 유일해야 한다 (UNIQUE).
- categoryId는 `ingredient_category` 테이블에 존재하는 값이어야 한다.
- defaultUnit은 허용된 enum 값(GRAM, MILLILITER, PIECE, TABLESPOON, TEASPOON, CUP) 중 하나여야 한다.

#### Scenario: 정상 등록
- **WHEN** 유효한 name, categoryId, defaultUnit을 포함한 POST /ingredients 요청을 보낼 때
- **THEN** 201 Created와 등록된 재료의 id, name, categoryId, defaultUnit, description을 반환한다

#### Scenario: 중복 name 등록 시도
- **WHEN** 이미 존재하는 name으로 POST /ingredients 요청을 보낼 때
- **THEN** 409 Conflict를 반환한다

#### Scenario: 존재하지 않는 categoryId로 등록 시도
- **WHEN** 존재하지 않는 categoryId로 POST /ingredients 요청을 보낼 때
- **THEN** 400 Bad Request를 반환한다

#### Scenario: defaultUnit 누락
- **WHEN** defaultUnit 없이 POST /ingredients 요청을 보낼 때
- **THEN** 400 Bad Request를 반환한다

### Requirement: 재료 단건 조회
시스템은 id로 재료 하나를 조회할 수 있어야 한다.

#### Scenario: 존재하는 재료 조회
- **WHEN** 존재하는 id로 GET /ingredients/{id} 요청을 보낼 때
- **THEN** 200 OK와 해당 재료의 전체 필드를 반환한다

#### Scenario: 존재하지 않는 재료 조회
- **WHEN** 존재하지 않는 id로 GET /ingredients/{id} 요청을 보낼 때
- **THEN** 404 Not Found를 반환한다

### Requirement: 재료 목록 조회
시스템은 페이징과 category 필터를 지원하는 목록 조회 API를 제공해야 한다.

#### Scenario: 전체 목록 페이징 조회
- **WHEN** GET /ingredients?page=0&size=20 요청을 보낼 때
- **THEN** 200 OK와 Page 형태의 재료 목록(content, totalElements, totalPages 포함)을 반환한다

#### Scenario: category 필터 적용
- **WHEN** GET /ingredients?categoryId=1&page=0&size=20 요청을 보낼 때
- **THEN** 해당 categoryId에 속하는 재료만 포함된 Page를 반환한다

#### Scenario: 결과가 없는 경우
- **WHEN** 조건에 맞는 재료가 없을 때
- **THEN** 200 OK와 빈 content 배열을 반환한다

### Requirement: 재료 수정
시스템은 id로 재료의 name, categoryId, defaultUnit, description을 수정할 수 있어야 한다.

#### Scenario: 정상 수정
- **WHEN** 존재하는 id와 유효한 필드로 PUT /ingredients/{id} 요청을 보낼 때
- **THEN** 200 OK와 수정된 재료 전체 필드를 반환한다

#### Scenario: 다른 재료와 중복되는 name으로 수정 시도
- **WHEN** 이미 다른 재료가 사용 중인 name으로 PUT /ingredients/{id} 요청을 보낼 때
- **THEN** 409 Conflict를 반환한다

#### Scenario: 존재하지 않는 재료 수정 시도
- **WHEN** 존재하지 않는 id로 PUT /ingredients/{id} 요청을 보낼 때
- **THEN** 404 Not Found를 반환한다

### Requirement: 재료 삭제
시스템은 id로 재료를 Hard delete할 수 있어야 한다.

#### Scenario: 정상 삭제
- **WHEN** 존재하는 id로 DELETE /ingredients/{id} 요청을 보낼 때
- **THEN** 204 No Content를 반환하고 해당 재료는 DB에서 제거된다

#### Scenario: 존재하지 않는 재료 삭제 시도
- **WHEN** 존재하지 않는 id로 DELETE /ingredients/{id} 요청을 보낼 때
- **THEN** 404 Not Found를 반환한다
