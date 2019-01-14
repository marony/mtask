package com.binbo_kodakusan.mtask.models.td

import play.api.libs.json._

case class Task(id: Int, title: String, modified: Int, completed: Option[Int])

object Task {
  implicit val format = Json.format[Task]
}
