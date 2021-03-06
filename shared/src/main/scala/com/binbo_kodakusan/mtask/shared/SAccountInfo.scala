package com.binbo_kodakusan.mtask.shared

import play.api.libs.json._

case class SAccountInfo(userid: String, alias: String, email: String, pro: Int,
                        dateformat: Int, timezone: Int, hidemonths: Int,
                        hotlistpriority: Int, hotlistduedate: Int, showtabnums: Int,
                        lastedit_folder: Int, lastedit_context: Int, lastedit_goal: Int,
                        lastedit_location: Int, lastedit_task: Int, lastdelete_task: Int,
                        lastedit_note: Int, lastdelete_note: Int, lastedit_list: Int, lastedit_outline: Int)

object SAccountInfo {
  implicit val format = Json.format[SAccountInfo]
}
