# users user_id to id

# --- !Ups

CREATE TABLE users_new (
  id VARCHAR(256) NOT NULL,
  alias VARCHAR(256) NOT NULL,
  email VARCHAR(256) NOT NULL,
  access_token VARCHAR(256) NOT NULL,
  refresh_token VARCHAR(256) NOT NULL,
  last_sync INT NOT NULL,
  last_edit_folder INT NOT NULL,
  last_edit_context INT NOT NULL,
  last_edit_goal INT NOT NULL,
  last_edit_location INT NOT NULL,
  last_edit_task INT NOT NULL,
  last_delete_task INT NOT NULL,
  last_edit_note INT NOT NULL,
  last_delete_note INT NOT NULL,
  last_edit_list INT NOT NULL,
  last_edit_outline INT NOT NULL,
  PRIMARY KEY (id)
);

INSERT INTO users_new
(id, alias, email, access_token,
 refresh_token, last_sync, last_edit_folder, last_edit_context, last_edit_goal,
 last_edit_location, last_edit_task, last_delete_task, last_edit_note,
 last_delete_note, last_edit_list, last_edit_outline)
SELECT user_id, alias, email, access_token,
       refresh_token, last_sync, last_edit_folder, last_edit_context, last_edit_goal,
       last_edit_location, last_edit_task, last_delete_task, last_edit_note,
       last_delete_note, last_edit_list, last_edit_outline
FROM users;

DROP TABLE users;

ALTER TABLE users_new RENAME TO users;

# --- !Downs

DROP TABLE IF EXISTS users_new;
