package com.binbo_kodakusan.mtask

import diode.Action

// アプリケーションモデル
case class AppModel(tasks: RTasks)
case class RTasks(tasks: Seq[shared.STask])

// アクション
case object InitTodos extends Action
case class GetTasks(tasks: Seq[shared.STask]) extends Action
case class AddTask(title: String) extends Action
case class ToggleAll(checked: Boolean) extends Action
case class ToggleCompleted(id: Int) extends Action
case class Update(id: Int, title: String) extends Action
case class Delete(id: Int) extends Action
