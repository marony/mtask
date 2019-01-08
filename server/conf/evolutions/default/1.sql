# users schema

# --- !Ups

CREATE TABLE users (
  id bigserial NOT NULL,
  email varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  userName varchar(255) NOT NULL,
  isAdmin boolean NOT NULL,
  PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE users;
