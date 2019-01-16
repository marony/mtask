package com.binbo_kodakusan.mtask.components

import japgolly.scalajs.react.vdom.html_<^.{<, ^}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

object Footer {

  case class Props(
                    activeCount: Int,
                    completedCount: Int
                  )

  class Backend($ : BackendScope[Props, Unit]) {
    def clearButton(p: Props) =
      <.button(
        ^.className := "clear-completed",
        "Clear completed",
        ^.visibility.hidden.when(p.completedCount == 0)
      )

    def render(p: Props) =
      <.footer(
        ^.className := "footer",
        <.span(
          ^.className := "todo-count",
          <.strong(p.activeCount),
          s" ${if (p.activeCount == 1) "item" else "items"} left"
        ),
        clearButton(p)
      )
  }

  private val component = ScalaComponent
    .builder[Props]("Footer")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(p: Props) = component(p)
}
