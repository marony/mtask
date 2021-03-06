package com.binbo_kodakusan.mtask

import boopickle.Default._
import com.binbo_kodakusan.mtask.containers.TaskList
import diode.dev.{Hooks, PersistStateIDB}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel, JSImport}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.Implicits._

import scala.scalajs.js.typedarray.TypedArrayBufferOps._
import scala.scalajs.js.typedarray._

@JSExportTopLevel("TodoMVC")
object TaskApp extends js.JSApp {

  val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))

  val routerConfig: RouterConfig[Unit] = RouterConfigDsl[Unit].buildConfig { dsl =>
    import dsl._

    val taskConnection = AppCircuit.connect(_.tasks)

    val route = staticRoute("#/", ()) ~> renderR(router => taskConnection(p => TaskList(p, router)))

    /* build a final RouterConfig with a default page */
    route.notFound(redirectToPage("")(Redirect.Replace))
  }

  /**
    * Function to pickle application model into a TypedArray
    *
    * @param model
    * @return
    */
  def pickle(model: AppModel) = {
    val data = Pickle.intoBytes(model)
    data.typedArray().subarray(data.position, data.limit)
  }

  /**
    * Function to unpickle application model from a TypedArray
    *
    * @param data
    * @return
    */
  def unpickle(data: Int8Array) = {
    Unpickle[AppModel].fromBytes(TypedArrayBuffer.wrap(data))
  }

  @JSExport
  override def main(): Unit = {
    println("Starting")
    // add a development tool to persist application state
    AppCircuit.addProcessor(new PersistStateIDB(pickle, unpickle))
    // hook it into Ctrl+Shift+S and Ctrl+Shift+L
    println("Hooking")
    Hooks.hookPersistState("test", AppCircuit)

    println("Init")
    AppCircuit.dispatch(InitTodos)
    /** The router is itself a React component, which at this point is not mounted */
    val router = Router(baseUrl, routerConfig.logToConsole)

    println("Render")
    router().renderIntoDOM(dom.document.getElementById("app").domAsHtml)
  }
}
