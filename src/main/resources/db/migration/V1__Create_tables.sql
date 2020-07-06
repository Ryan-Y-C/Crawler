create table LINKS_TO_BE_PROCESSED
(
    link varchar(1000)
) default charset = utf8mb4;
create table LINKS_ALREADY_PROCESSED
(
    link varchar(1000)
) default charset = utf8mb4;
create table NEWS
(
    id          bigint primary key AUTO_INCREMENT,
    title       text,
    content     text,
    url         varchar(1000),
    created_at  timestamp default now(),
    modified_at timestamp default now()
) default charset = utf8mb4;
