CREATE TABLE ingredient_category (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ingredient_category_name (name)
);

CREATE TABLE ingredient (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(200) NOT NULL,
    category_id  BIGINT,
    default_unit VARCHAR(50)  NOT NULL,
    description  TEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ingredient_name (name)
);

INSERT INTO ingredient_category (name) VALUES
    ('채소'),
    ('육류'),
    ('해산물'),
    ('유제품'),
    ('양념/소스'),
    ('곡물'),
    ('기타');
