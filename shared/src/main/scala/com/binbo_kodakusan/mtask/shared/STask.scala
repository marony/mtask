package com.binbo_kodakusan.mtask.shared

import play.api.libs.json._

case class STask(id: Int, title: String)

object STask {
  implicit val format = Json.format[STask]
}
