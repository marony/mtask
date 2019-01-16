package com.binbo_kodakusan.mtask.components

import com.binbo_kodakusan.mtask.shared
import japgolly.scalajs.react.vdom.html_<^.{<, VdomElement, ^}
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ReactKeyboardEvent, ScalaComponent}
import org.scalajs.dom.ext.KeyCode

object TaskView {

  case class Props(onToggle: Callback,
                   onDelete: Callback,
                   onStartEditing: Callback,
                   onUpdateTitle: String => Callback,
                   onCancelEditing: Callback,
                   task: shared.Task,
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
      <.li(
        ^.classSet(
          "completed" -> false/*p.task.isCompleted*/,
          "editing"   -> p.isEditing
        ),
        <.div(
          ^.className := "view",
          <.input.checkbox(
            ^.className := "toggle",
            ^.checked := false/*p.task.isCompleted*/,
            ^.onChange --> p.onToggle
          ),
          <.label(
            p.task.title,
            ^.onDoubleClick --> p.onStartEditing
          ),
          <.button(
            ^.className := "destroy",
            ^.onClick --> p.onDelete
          )
        ),
        <.input(
          ^.className := "edit",
          ^.onBlur --> editFieldSubmit(p),
          ^.onChange ==> editFieldChanged,
          ^.onKeyDown ==>? editFieldKeyDown(p),
          ^.value := s.editText
        )
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
