package com.binbo_kodakusan.mtask.shared

import play.api.libs.json._
import upickle.default.{ReadWriter => RW, macroRW}

case class STask(id: Int, title: String)

object STask {
  implicit val format = Json.format[STask]
  implicit val rw: RW[STask] = macroRW
}
