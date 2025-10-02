--spring 서버 실행시 생성될 DB의 데이터 초기화
-- 테이블 자동생성 옵션 : spring.jpa.hibernate.ddl-auto=create-drop, spring.jpa.defer-datasource-initialization=true

insert into users (username, password, email, activated)
values ('admin', '$2a$08$lDnHPz7eUkSi6ao14Twuau08mzhWrL4kyZGGU5xfiGALO/Vxd5DOi', 'admin@n.com', 1),
       ('user', '$2a$08$UkVvwpULis18S19S5pZFn.YHPZt3oaqHZnDwqbCW9pft6uFtkXKDC', 'user@g.com', 1),
       ('guest', '$2a$08$UkVvwpULis18S19S5pZFn.YHPZt3oaqHZnDwqbCW9pft6uFtkXKDC', 'guest@g.com', 1);

insert into authority (authority_name)
values ('ROLE_USER'),
       ('ROLE_ADMIN'),
       ('WRITE'),
       ('DELETE');

--admin 은 CRUD, user 는 CRU, guest 는 R
insert into user_authority (user_id, authority_name)
values (1, 'ROLE_USER'),
       (1, 'ROLE_ADMIN'),
       (1, 'WRITE'),
       (1, 'DELETE'),
       (2, 'ROLE_USER'),
       (2, 'WRITE'),
       (3, 'ROLE_USER');

-- 1. 테이블 생성
CREATE TABLE IF NOT EXISTS userlist (
     id VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    display_name VARCHAR(100),
    email VARCHAR(100),
    type INT,
    timezone VARCHAR(50),
    verified INT,
    dept VARCHAR(100),
    created_at TIMESTAMP,
    last_login_time TIMESTAMP,
    last_client_version VARCHAR(50),
    language VARCHAR(10),
    status VARCHAR(20),
    role_id VARCHAR(10),
    user_created_at TIMESTAMP
    );
