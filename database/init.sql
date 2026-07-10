-- Docker Compose runs files in /docker-entrypoint-initdb.d on first database creation.
-- docker-compose.yml mounts this file before database/schema.sql so the schema is
-- initialized automatically when the persistent Postgres volume is first created.
SELECT 'InsightFlow PostgreSQL initialization started' AS message;
