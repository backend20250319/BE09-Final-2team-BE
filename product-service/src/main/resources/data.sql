-- 최상위 카테고리
INSERT INTO tbl_product_category (name, parent_id, created_at, updated_at, create_by, update_by)
VALUES ('출산/육아용품', NULL, NOW(), NOW(), 1, 1),
       ('유아동의류', NULL, NOW(), NOW(), 1, 1),
       ('유아동잡화', NULL, NOW(), NOW(), 1, 1),
       ('유아동교구/완구', NULL, NOW(), NOW(), 1, 1),
       ('유아동안전/실내용품', NULL, NOW(), NOW(), 1, 1),
       ('유아동가구', NULL, NOW(), NOW(), 1, 1),
       ('기타 유아동용품', NULL, NOW(), NOW(), 1, 1);

-- 예: "출산/육아용품"의 자식들
INSERT INTO tbl_product_category (name, parent_id, created_at, updated_at, create_by, update_by)
VALUES ('분유수유용품', 1, NOW(), NOW(), 1, 1),
       ('튼살크림/스킨케어', 1, NOW(), NOW(), 1, 1),
       ('임부복/수유복/언더웨어', 1, NOW(), NOW(), 1, 1),
       ('물티슈/기저귀', 1, NOW(), NOW(), 1, 1),
       ('분유/이유식', 1, NOW(), NOW(), 1, 1),
       ('아기띠/기저귀가방', 1, NOW(), NOW(), 1, 1),
       ('신생아/영유아의류', 1, NOW(), NOW(), 1, 1),
       ('유아로션/목욕용품', 1, NOW(), NOW(), 1, 1),
       ('유아건강/위생용품', 1, NOW(), NOW(), 1, 1),
       ('유모차/웨건', 1, NOW(), NOW(), 1, 1);

-- "유아동의류"의 자식들
INSERT INTO tbl_product_category (name, parent_id, created_at, updated_at, create_by, update_by)
VALUES ('유아용의류', 2, NOW(), NOW(), 1, 1),
       ('아동용의류', 2, NOW(), NOW(), 1, 1),
       ('내의/잠옷/속옷', 2, NOW(), NOW(), 1, 1),
       ('패딩/자켓', 2, NOW(), NOW(), 1, 1),
       ('한복/소품', 2, NOW(), NOW(), 1, 1);

-- "유아동잡화"의 자식들
INSERT INTO tbl_product_category (name, parent_id, created_at, updated_at, create_by, update_by)
VALUES ('구두/운동화/샌들/부츠', 3, NOW(), NOW(), 1, 1),
       ('장화/우비/우산', 3, NOW(), NOW(), 1, 1),
       ('모자/장갑', 3, NOW(), NOW(), 1, 1),
       ('책가방/여행가방', 3, NOW(), NOW(), 1, 1);

-- "유아동교구/완구"의 자식들
INSERT INTO tbl_product_category (name, parent_id, created_at, updated_at, create_by, update_by)
VALUES ('신생아완구', 4, NOW(), NOW(), 1, 1),
       ('원목교구', 4, NOW(), NOW(), 1, 1),
       ('음악놀이/자석교구', 4, NOW(), NOW(), 1, 1),
       ('전동차/핫휠', 4, NOW(), NOW(), 1, 1),
       ('로봇', 4, NOW(), NOW(), 1, 1),
       ('인형/디즈니의상', 4, NOW(), NOW(), 1, 1),
       ('블록/레고', 4, NOW(), NOW(), 1, 1),
       ('대형 완구용품', 4, NOW(), NOW(), 1, 1);

-- "유아동안전/실내용품"의 자식
INSERT INTO tbl_product_category (name, parent_id, created_at, updated_at, create_by, update_by)
VALUES ('카시트', 5, NOW(), NOW(), 1, 1);

-- "유아동가구"의 자식
INSERT INTO tbl_product_category (name, parent_id, created_at, updated_at, create_by, update_by)
VALUES ('침대/매트리스', 6, NOW(), NOW(), 1, 1);
