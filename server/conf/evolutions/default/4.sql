# tasks index

# --- !Ups

CREATE UNIQUE INDEX IX_users_user_id ON users (user_id);

# --- !Downs

DROP INDEX IX_users_user_id;
