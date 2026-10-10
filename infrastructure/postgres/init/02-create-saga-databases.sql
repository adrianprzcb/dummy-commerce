SELECT 'CREATE USER orders_user WITH PASSWORD ''orders_password'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'orders_user')
\gexec

SELECT 'CREATE DATABASE orders_db OWNER orders_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orders_db')
\gexec

SELECT 'CREATE USER inventory_user WITH PASSWORD ''inventory_password'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'inventory_user')
\gexec

SELECT 'CREATE DATABASE inventory_db OWNER inventory_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory_db')
\gexec

SELECT 'CREATE USER payments_user WITH PASSWORD ''payments_password'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'payments_user')
\gexec

SELECT 'CREATE DATABASE payments_db OWNER payments_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payments_db')
\gexec
