# tasks schema

# --- !Ups

CREATE TABLE tasks (
  id bigserial NOT NULL,
  task_id VARCHAR(255) NOT NULL,
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

CREATE UNIQUE INDEX IX_tasks_task_id ON tasks (task_id);

# --- !Downs

DROP TABLE tasks;
