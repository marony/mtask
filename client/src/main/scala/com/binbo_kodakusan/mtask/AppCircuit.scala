package com.binbo_kodakusan.mtask

import diode._
import diode.react.ReactConnector
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom

import scala.util.{Failure, Success}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import upickle.default._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * AppCircuit provides the actual instance of the `AppModel` and all the action
  * handlers we need. Everything else comes from the `Circuit`
  */
object AppCircuit extends Circuit[AppModel] with ReactConnector[AppModel] {
  // アプリケーションモデルの初期データ
  def initialModel = AppModel(RTasks(Seq()))

  override val actionHandler = composeHandlers(
    // タスクにZoom
    new TaskHandler(zoomTo(_.tasks.tasks))
    // TODO: 他のデータを触るにはどうするの？
  )
}

/**
  * タスク用ハンドラ
  *
  * @param modelRW
  * @tparam M
  */
class TaskHandler[M](modelRW: ModelRW[M, Seq[shared.STask]]) extends ActionHandler(modelRW) {
  /**
    * TODO: これはなに？
    *
    * @param Id
    * @param f
    * @return
    */
  def updateOne(Id: Int)(f: shared.STask => shared.STask): Seq[shared.STask] =
    value.map {
      case found@shared.STask(Id, _) => f(found)
      case other => other
    }

  /**
    * TODO: アクションを処理する
    * @return
    */
  override def handle = {
    case InitTodos => {
      // TODO: 初期データをサーバから取得する
      println("Initializing todos")
      effectOnly(Effect(Ajax.get("http://localhost:9000/td_get_tasks").map { xhr =>
        GetTasks(read[Seq[shared.STask]](xhr.responseText))
      }))
    }
    case GetTasks(tasks) =>
      updated(tasks)
    case AddTask(title) =>
      // TODO: 追加する
      updated(value :+ shared.STask(1, title))
//    case ToggleAll(checked) =>
//      updated(value.map(_.copy(isCompleted = checked)))
//    case ToggleCompleted(id) =>
//      updated(updateOne(id)(old => old.copy(isCompleted = !old.isCompleted)))
    case Update(id, title) =>
      // TODO: 更新する
      updated(updateOne(id)(_.copy(title = title)))
    case Delete(id) =>
      // TODO: 削除する
      updated(value.filterNot(_.id == id))
  }
}
