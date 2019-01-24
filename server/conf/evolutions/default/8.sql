# users カラム定義変更

# --- !Ups

ALTER TABLE users ALTER COLUMN access_token DROP NOT NULL;
ALTER TABLE users ALTER COLUMN refresh_token DROP NOT NULL;

# --- !Downs

ALTER TABLE users ALTER COLUMN access_token SET NOT NULL;
ALTER TABLE users ALTER COLUMN refresh_token SET NOT NULL;
