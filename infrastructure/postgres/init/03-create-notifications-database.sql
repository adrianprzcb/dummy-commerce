SELECT 'CREATE USER notifications_user WITH PASSWORD ''notifications_password'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'notifications_user')
\gexec

SELECT 'CREATE DATABASE notifications_db OWNER notifications_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notifications_db')
\gexec
