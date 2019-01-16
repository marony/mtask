package com.binbo_kodakusan.mtask.containers

import diode.react.ModelProxy
import diode.Action

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.ext.KeyCode

import com.binbo_kodakusan.mtask._
import com.binbo_kodakusan.mtask.components.{Footer, TaskView}

object TaskList {

  case class Props(proxy: ModelProxy[Tasks], ctl: RouterCtl[Unit])

  case class State(editing: Option[Int])

  class Backend($ : BackendScope[Props, State]) {
    def mounted(props: Props) = Callback {}

    def handleNewTodoKeyDown(dispatch: Action => Callback)(e: ReactKeyboardEventFromInput): Option[Callback] = {
      val title = e.target.value.trim
      if (e.nativeEvent.keyCode == KeyCode.Enter && title.nonEmpty) {
        Some(Callback(e.target.value = "") >> dispatch(AddTask(title)))
      } else {
        None
      }
    }

    def editingDone(): Callback =
      $.modState(_.copy(editing = None))

    val startEditing: Int => Callback =
      id => $.modState(_.copy(editing = Some(id)))

    /**
      * メインコンポーネント
      *
      * @param p
      * @param s
      * @return
      */
    def render(p: Props, s: State) = {
      val proxy                        = p.proxy()
      val dispatch: Action => Callback = p.proxy.dispatchCB
      val tasks                        = proxy.tasks
      val activeCount                  = tasks count (t => t.id == 1)
      val completedCount               = tasks.length - activeCount

      <.div(
        <.h1("tasks"),
        <.header(
          ^.className := "header",
          <.input(
            ^.className := "new-todo",
            ^.placeholder := "What needs to be done?",
            ^.onKeyDown ==>? handleNewTodoKeyDown(dispatch),
            ^.autoFocus := true
          )
        ),
        taskList(dispatch, s.editing, tasks, activeCount).when(tasks.nonEmpty),
        footer(p, dispatch, activeCount, completedCount).when(tasks.nonEmpty)
      )
    }

    /**
      * タスクリスト
      *
      * @param dispatch
      * @param editing
      * @param todos
      * @param activeCount
      * @return
      */
    def taskList(dispatch: Action => Callback, editing: Option[Int], todos: Seq[shared.Task], activeCount: Int) =
      <.section(
        ^.className := "main",
        <.input.checkbox(
          ^.className := "toggle-all",
          ^.checked := activeCount == 0,
          ^.onChange ==> { e: ReactEventFromInput =>
            dispatch(ToggleAll(e.target.checked))
          }
        ),
        <.ul(
          ^.className := "todo-list",
          todos.toTagMod(
            task =>
              TaskView(TaskView.Props(
                onToggle = dispatch(ToggleCompleted(task.id)),
                onDelete = dispatch(Delete(task.id)),
                onStartEditing = startEditing(task.id),
                onUpdateTitle = title => dispatch(Update(task.id, title)) >> editingDone(),
                onCancelEditing = editingDone(),
                task = task,
                isEditing = editing.contains(task.id)
              )))
        )
      )

    def footer(p: Props,
               dispatch: Action => Callback,
               activeCount: Int,
               completedCount: Int): VdomElement =
      Footer(
        Footer.Props(
          activeCount = activeCount,
          completedCount = completedCount
        ))
  }

  private val component = ScalaComponent
    .builder[Props]("TodoList")
    .initialStateFromProps(p => State(None))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .build

  def apply(proxy: ModelProxy[Tasks], ctl: RouterCtl[Unit]) =
    component(Props(proxy, ctl))
}
