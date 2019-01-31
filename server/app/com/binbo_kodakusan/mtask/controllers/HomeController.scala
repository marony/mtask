package com.binbo_kodakusan.mtask.controllers

import com.binbo_kodakusan.mtask.Constants
import com.binbo_kodakusan.mtask.models.{SessionState, Tables}
import javax.inject._
import play.api.{Configuration, Logger}
import play.api.mvc._
import com.binbo_kodakusan.mtask.services.{ToodledoApi, ToodledoService, UserQuery, UserService}
import com.binbo_kodakusan.mtask.util.{Logging, SessionUtil}
import play.api.i18n.Messages

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

@Singleton
class HomeController @Inject()
  (config: Configuration, mcc: MessagesControllerComponents)
  (userDAO: UserQuery)
  (implicit ec: ExecutionContext,
   tdService: ToodledoService,
   webJarsUtil: org.webjars.play.WebJarsUtil)
    extends MessagesAbstractController(mcc) {

  /**
    * トップページ
    *
    * @return
    */
  def index = Action.async { implicit request: MessagesRequest[AnyContent] =>
    Logging("HomeController.index", {
      val et = tdService.loginFromSession()
      val f = et.value.map {
        case Right(state) =>
          // うまくいったらReactのアプリを表示
          val session = SessionUtil.setTdSession(
            // stateをセッションから削除
            SessionUtil.remove(request.session, Constants.SessionName.TD_STATE),
            // セッションに色々設定
            state)
          Redirect(routes.HomeController.app)
            .withSession(session)
        case Left(e) =>
          Logger.error(e.toString)
          Ok(views.html.index("タイトルだよ"))
            .flashing("danger" -> Messages("toodledo.not_login"))
            .withNewSession
      }
      f
    })
  }

  /**
    * Reactアプリ
    * @return
    */
  def app = Action { implicit request: MessagesRequest[AnyContent] =>
    Logging("HomeController.app", {
      val stateOpt = SessionUtil.getTdSessionState(request.session)
      stateOpt match {
        case Some(state) =>
          Ok(views.html.app("アプリだよ", "Message"))
        case None =>
          Redirect(routes.HomeController.index)
            .flashing("danger" -> Messages("toodledo.not_login"))
            .withNewSession
      }
    })
  }
}
