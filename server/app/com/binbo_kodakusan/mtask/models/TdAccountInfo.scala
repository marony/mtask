package com.binbo_kodakusan.mtask.models

import com.binbo_kodakusan.mtask.shared
import play.api.libs.json._

case class TdAccountInfo(userid: String, alias: String, email: String, pro: Int,
                         dateformat: Int, timezone: Int, hidemonths: Int,
                         hotlistpriority: Int, hotlistduedate: Int, showtabnums: Int,
                         lastedit_folder: Int, lastedit_context: Int, lastedit_goal: Int,
                         lastedit_location: Int, lastedit_task: Int, lastdelete_task: Int,
                         lastedit_note: Int, lastdelete_note: Int, lastedit_list: Int, lastedit_outline: Int) {
  def toShared(): shared.SAccountInfo = {
    shared.SAccountInfo(userid, alias, email, pro,
      dateformat, timezone, hidemonths,
      hotlistpriority, hotlistduedate, showtabnums,
      lastedit_folder, lastedit_context, lastedit_goal,
      lastedit_location, lastdelete_task, lastdelete_task,
      lastedit_note, lastdelete_note, lastedit_list, lastedit_outline)
  }
}

object TdAccountInfo {
  implicit val format = Json.format[TdAccountInfo]
}
