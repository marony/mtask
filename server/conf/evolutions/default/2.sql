# users alias追加

# --- !Ups

CREATE TABLE users_new (
  id bigserial NOT NULL,
  user_id VARCHAR(256) NOT NULL,
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
(user_id, alias, email, access_token,
 refresh_token, last_sync, last_edit_folder, last_edit_context, last_edit_goal,
 last_edit_location, last_edit_task, last_delete_task, last_edit_note,
 last_delete_note, last_edit_list, last_edit_outline)
SELECT userid, 'alias', email, access_token,
       refresh_token, last_sync, lastedit_folder, lastedit_context, lastedit_goal,
       lastedit_location, lastedit_task, lastdelete_task, lastedit_note,
       lastdelete_note, lastedit_list, lastedit_outline
FROM users;

DROP TABLE users;

ALTER TABLE users_new RENAME TO users;

# --- !Downs

ALTER TABLE users DROP COLUMN alias;
