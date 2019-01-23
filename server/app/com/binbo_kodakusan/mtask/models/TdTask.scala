package com.binbo_kodakusan.mtask.models

import play.api.libs.json._

case class TdTask(id: Int, title: String, modified: Int, completed: Int,
                  folder: Int, context: Int, goal: Int, location: Int,
                  tag: String, startdate: Int, duedate: Int,
                  remind: Int, repeat: String,
                  status: Int, star: Int, priority: Int,
                  added: Int, note: String, parent: Int, children: Int, order: Int,
                  meta: Option[String]) {

  def toShared(): com.binbo_kodakusan.mtask.shared.STask = {
    com.binbo_kodakusan.mtask.shared.STask(id, title)
  }
}

object TdTask {
  implicit val format = Json.format[TdTask]
}
