package com.binbo_kodakusan.mtask

import diode.Action

// アプリケーションモデル
case class AppModel(tasks: Tasks)
case class Tasks(tasks: Seq[shared.Task])

// アクション
case object InitTodos extends Action
case class GetTasks(tasks: Seq[shared.Task]) extends Action
case class AddTask(title: String) extends Action
case class ToggleAll(checked: Boolean) extends Action
case class ToggleCompleted(id: Int) extends Action
case class Update(id: Int, title: String) extends Action
case class Delete(id: Int) extends Action
