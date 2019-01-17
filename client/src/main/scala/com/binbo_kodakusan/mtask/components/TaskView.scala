package com.binbo_kodakusan.mtask.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import org.scalajs.dom.ext.KeyCode
import com.binbo_kodakusan.mtask.shared

object TaskView {

  case class Props(onToggle: Callback,
                   onDelete: Callback,
                   onStartEditing: Callback,
                   onUpdateTitle: String => Callback,
                   onCancelEditing: Callback,
                   task: shared.STask,
                   isEditing: Boolean)

  case class State(editText: String)

  class Backend($ : BackendScope[Props, State]) {
    val x = $.props.map(_.isEditing)
    def editFieldSubmit(p: Props): Callback =
      $.state.flatMap(
        s =>
          if (s.editText.trim == "")
            p.onDelete
          else p.onUpdateTitle(s.editText.trim))

    def resetText(p: Props): Callback =
      $.modState(_.copy(editText = p.task.title))

    def editFieldKeyDown(p: Props): ReactKeyboardEvent => Option[Callback] =
      e =>
        e.nativeEvent.keyCode match {
          case KeyCode.Escape => Some(resetText(p) >> p.onCancelEditing)
          case KeyCode.Enter  => Some(editFieldSubmit(p))
          case _              => None
        }

    val editFieldChanged: ReactEventFromInput => Callback =
      e => Callback { e.persist() } >> $.modState(_.copy(editText = e.target.value))

    def render(p: Props, s: State): VdomElement = {
      <.tr(
        ^.classSet(
          "completed" -> false/*p.task.isCompleted*/,
          "editing"   -> p.isEditing
        ),
        <.td(
          p.task.title,
          ^.onDoubleClick --> p.onStartEditing
        ),
        <.td(
          <.button(
            ^.className := "destroy",
            ^.onClick --> p.onDelete,
            "ボタン"
          ))
      )
    }
  }

  val component = ScalaComponent
    .builder[Props]("CTodoItem")
    .initialStateFromProps(p => State(p.task.title))
    .renderBackend[Backend]
    .build

  def apply(P: Props) =
    component.withKey(P.task.id)(P)
}
