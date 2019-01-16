package com.binbo_kodakusan.mtask.models.td

import com.binbo_kodakusan.mtask.shared
import play.api.libs.json._

case class Task(id: Int, title: String, modified: Int, completed: Int) {
  def toShared(): shared.Task = {
    shared.Task(id, title)
  }
}

object Task {
  implicit val format = Json.format[Task]
}
