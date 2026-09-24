CREATE USER store_user WITH PASSWORD 'store_password';
CREATE DATABASE store_db OWNER store_user;

CREATE USER users_user WITH PASSWORD 'users_password';
CREATE DATABASE users_db OWNER users_user;