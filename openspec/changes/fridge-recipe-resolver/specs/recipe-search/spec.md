## ADDED Requirements

### Requirement: 레시피 Document 구조
시스템은 Elasticsearch에 저장하는 `RecipeDocument`에 id, name, description, ingredients 배열(각 항목 id/name/essential), cookingTime, difficulty 필드를 포함해야 한다(SHALL). `description` 필드는 text 타입으로 키워드 검색 대상에 포함해야 한다(SHALL). 향후 임베딩 기반 시맨틱 검색 확장을 위해 `description` 필드는 현 단계부터 필수 포함되어야 한다(SHALL).

#### Scenario: 레시피 동기화 시 description 포함
- **WHEN** 레시피가 RDB에 저장/수정되고 ES에 동기화되는 경우
- **THEN** 시스템은 `RecipeDocument.description` 필드에 RDB의 `recipe.description` 값을 저장한다

#### Scenario: description 누락 시 처리
- **WHEN** 레시피에 description 값이 비어 있는 경우
- **THEN** 시스템은 빈 문자열로 저장하고 인덱싱을 계속 진행한다

### Requirement: 레시피명 전문 검색
시스템은 Elasticsearch를 사용하여 레시피명으로 전문 검색을 제공해야 한다(SHALL).

#### Scenario: 레시피명 검색
- **WHEN** 사용자가 "김치찌개"로 검색 요청
- **THEN** 시스템은 레시피명에 "김치찌개"가 포함된 레시피 목록을 관련도 순으로 반환한다

#### Scenario: 검색 결과 없음
- **WHEN** 사용자가 매칭되지 않는 검색어로 검색 요청
- **THEN** 시스템은 빈 목록을 반환한다

### Requirement: 레시피 설명 키워드 검색
시스템은 `description` 필드에 포함된 키워드로도 레시피를 검색할 수 있어야 한다(SHALL).

#### Scenario: description 키워드 매칭
- **WHEN** 사용자가 "매콤한"으로 검색 요청하고 해당 단어가 레시피 description에 포함된 경우
- **THEN** 시스템은 해당 레시피를 검색 결과에 포함시킨다

### Requirement: 재료명 기반 검색
시스템은 재료명으로 해당 재료를 사용하는 레시피를 검색할 수 있어야 한다(SHALL).

#### Scenario: 단일 재료 검색
- **WHEN** 사용자가 재료 "토마토"로 검색 요청
- **THEN** 시스템은 토마토를 사용하는 레시피 목록을 반환한다

### Requirement: 복합 조건 검색
시스템은 포함 재료와 제외 재료를 동시에 지정하는 복합 조건 검색을 제공해야 한다(SHALL).

#### Scenario: 포함/제외 재료 조합 검색
- **WHEN** 사용자가 포함 재료 [토마토, 양파], 제외 재료 [고추]로 검색 요청
- **THEN** 시스템은 토마토와 양파를 포함하면서 고추를 포함하지 않는 레시피를 반환한다

#### Scenario: 포함 재료만 지정
- **WHEN** 사용자가 포함 재료만 지정하여 검색 요청
- **THEN** 시스템은 해당 재료를 모두 포함하는 레시피를 반환한다

#### Scenario: 제외 재료만 지정
- **WHEN** 사용자가 제외 재료만 지정하여 검색 요청
- **THEN** 시스템은 해당 재료를 포함하지 않는 전체 레시피를 반환한다

### Requirement: 재료 자동완성
시스템은 재료 입력 시 자동완성 기능을 제공해야 한다(SHALL). Elasticsearch의 Completion Suggester를 사용해야 한다(SHALL).

#### Scenario: 자동완성 제안
- **WHEN** 사용자가 "토마"를 입력
- **THEN** 시스템은 "토마토", "토마토소스" 등 매칭되는 재료명을 반환한다

#### Scenario: 자동완성 결과 없음
- **WHEN** 사용자가 매칭되지 않는 접두어를 입력
- **THEN** 시스템은 빈 제안 목록을 반환한다

### Requirement: RDB-ES 데이터 동기화
시스템은 레시피 데이터의 RDB 변경을 Elasticsearch에 동기화해야 한다(SHALL). 동기화는 이벤트 기반(@TransactionalEventListener)으로 트랜잭션 커밋 후 수행해야 한다(SHALL). 동기화 페이로드에는 `description` 필드가 포함되어야 한다(SHALL).

#### Scenario: 레시피 생성 시 동기화
- **WHEN** 새 레시피가 RDB에 저장되고 트랜잭션이 커밋된 경우
- **THEN** 시스템은 해당 레시피(description 포함)를 ES 인덱스에 추가한다

#### Scenario: 레시피 수정 시 동기화
- **WHEN** 기존 레시피가 RDB에서 수정되고 트랜잭션이 커밋된 경우
- **THEN** 시스템은 ES 인덱스의 해당 문서를 업데이트한다 (description 변경 포함)

#### Scenario: 레시피 삭제 시 동기화
- **WHEN** 레시피가 RDB에서 삭제되고 트랜잭션이 커밋된 경우
- **THEN** 시스템은 ES 인덱스에서 해당 문서를 삭제한다

#### Scenario: ES 동기화 실패 시 재시도
- **WHEN** ES 인덱싱이 실패한 경우
- **THEN** 시스템은 최대 3회까지 재시도하고, 최종 실패 시 로그를 기록한다
