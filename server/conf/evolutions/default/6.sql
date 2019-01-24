# tasks task_id to id

# --- !Ups

CREATE TABLE tasks_new (
  id VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  last_sync INT NOT NULL,
  modified INT NOT NULL,
  completed INT NOT NULL,
  folder_id INT NOT NULL,
  context_id INT NOT NULL,
  goal_id INT NOT NULL,
  location_id INT NOT NULL,
  tag VARCHAR(255) NOT NULL,
  start_date INT NOT NULL,
  due_date INT NOT NULL,
  remind INT NOT NULL,
  repeat VARCHAR(255) NOT NULL,
  status INT NOT NULL,
  star INT NOT NULL,
  priority INT NOT NULL,
  added INT NOT NULL,
  note TEXT NOT NULL,
  parent_id INT NOT NULL,
  children_count INT NOT NULL,
  order_no INT NOT NULL,
  meta TEXT NULL,
  PRIMARY KEY (id)
);

INSERT INTO tasks_new
(id, title, last_sync, modified, completed,
 folder_id, context_id, goal_id, location_id,
 tag, start_date, due_date, remind, repeat,
 status, star, priority, added, note,
 parent_id, children_count, order_no, meta)
SELECT task_id, title, last_sync, modified, completed,
       folder_id, context_id, goal_id, location_id,
       tag, start_date, due_date, remind, repeat,
       status, star, priority, added, note,
       parent_id, children_count, order_no, meta
FROM tasks;

DROP TABLE tasks;

ALTER TABLE tasks_new RENAME TO tasks;

# --- !Downs

DROP TABLE IF EXISTS users_new;
