package com.binbo_kodakusan.mtask.shared

import play.api.libs.json._
import upickle.default.{ReadWriter => RW, macroRW}

case class Task(id: Int, title: String)

object Task {
  implicit val format = Json.format[Task]
  implicit val rw: RW[Task] = macroRW
}
