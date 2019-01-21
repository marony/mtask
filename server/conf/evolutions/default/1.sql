# users schema

# --- !Ups

CREATE TABLE users (
  id bigserial NOT NULL,
  userid VARCHAR(256) NOT NULL,
  email VARCHAR(256) NOT NULL,
  access_token VARCHAR(256) NOT NULL,
  refresh_token VARCHAR(256) NOT NULL,
  last_sync INT NOT NULL,
  lastedit_folder INT NOT NULL,
  lastedit_context INT NOT NULL,
  lastedit_goal INT NOT NULL,
  lastedit_location INT NOT NULL,
  lastedit_task INT NOT NULL,
  lastdelete_task INT NOT NULL,
  lastedit_note INT NOT NULL,
  lastdelete_note INT NOT NULL,
  lastedit_list INT NOT NULL,
  lastedit_outline INT NOT NULL,
  PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE users;
