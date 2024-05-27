-- noinspection SqlDialectInspectionForFile

-- noinspection SqlNoDataSourceInspectionForFile

DROP TABLE IF EXISTS area;
create table area
(
    id       int
        primary key,
    province varchar(255) null,
    city     varchar(255) null,
    area     varchar(255) null,
    postcode varchar(255) null,
    del      boolean
);

DROP TABLE IF EXISTS "user";
create table "user"
(
    id          int
        primary key,
    pid         int null,
    "name"      varchar(255) null,
    "json"      varchar(255) null,
    address_id  int null,
    address_id2 int null,
    sex         int null,
    head_img    varchar(255) null,
    create_time timestamp null,
    create_by   int null,
    update_by   int null,
    del         boolean
);

DROP TABLE IF EXISTS address;
create table address
(
    id      int
        primary key,
    user_id int null,
    area_id int null,
    tel     varchar(255) null,
    address varchar(255) null,
    del     boolean
);

DROP TABLE IF EXISTS user_dto;
create table user_dto
(
    id        int
        primary key,
    user_id   int null,
    create_by int null,
    update_by int null,
    del       boolean null
);

DROP TABLE IF EXISTS order_t;
create table order_t
(
    id      int
        primary key,
    user_id int null,
    age     int null,
    name    varchar(255) null
);

DROP TABLE IF EXISTS user_tenant;
create table user_tenant
(
    id        int
        primary key,
    user_id   int null,
    tenant_id int null
);

DROP TABLE IF EXISTS user_tenanta;
create table user_tenanta
(
    id        int
        primary key,
    user_id   int null,
    tenant_id int null,
    中文字段  varchar(255) null
);