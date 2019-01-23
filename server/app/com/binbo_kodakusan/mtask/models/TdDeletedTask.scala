package com.binbo_kodakusan.mtask.models

import play.api.libs.json._

case class TdDeletedTask(id: Int, stamp: Int)

object TdDeletedTask {
  implicit val format = Json.format[TdDeletedTask]
}
