package com.binbo_kodakusan.mtask.controllers

import javax.inject._
import com.binbo_kodakusan.mtask.Constants
import com.binbo_kodakusan.mtask.models.{SessionState, TdAccountInfo}
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc._
import play.api.{Configuration, Logger}
import com.binbo_kodakusan.mtask.services.{ToodledoApi, ToodledoService, UserService}
import com.binbo_kodakusan.mtask.util._
import cats.data._
import cats.implicits._
import play.api.i18n.Messages

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ToodledoController @Inject()
  (mcc: MessagesControllerComponents,
   userService: UserService,
   tdApi: ToodledoApi, tdService: ToodledoService)
  (implicit ec: ExecutionContext, config: Configuration, ws: WSClient)
    extends MessagesAbstractController(mcc) {

  /**
    * Toodledoの認証
    * Toodledoのログインページにリダイレクトする
    *
    * @return
    */
  def authorize() = Action { implicit request: MessagesRequest[AnyContent] =>
    Logging("ToodledoController.authorize", {
      val url = config.get[String]("toodledo.authorize.url")
      val client_id = config.get[String]("toodledo.client_id")
      val state = java.util.UUID.randomUUID.toString
      val scope = config.get[String]("toodledo.authorize.scope")
      val device = request.headers("User-Agent")

      // stateをセッションに保存
      val session = SessionUtil.add(request.session,
        Constants.SessionName.TD_STATE -> state)
      Redirect(url, Map(
        "response_type" -> Seq("code"),
        "client_id" -> Seq(client_id),
        "state" -> Seq(state),
        "scope" -> Seq(scope),
        "device" -> Seq(device)
      )).withSession(session)
    })
  }

  /**
    * Toodledoからの認証コールバック
    * アクセストークンを取得する
    *
    * @param code
    * @param state
    * @param error
    * @return
    */
  def callback(code: String, state: String, error: Option[String]) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    Logging("ToodledoController.callback", {
      Logger.info(s"ToodledoController::callback called: code = $code, state = $state, error = $error")

      val oldState = request.session.get(Constants.SessionName.TD_STATE)

      val clientId = config.get[String]("toodledo.client_id")
      val secret = config.get[String]("toodledo.secret")
      val device = request.headers("User-Agent")
      // まだat_token_tookはない可能性がある
      val atTokenTook = request.session.get(Constants.SessionName.TD_AT_TOKEN_TOOK).map(_.toLong)

      val et = tdService.login(oldState, state, error, code, clientId, secret, device, atTokenTook)
      val f = et.value.map {
        case Right(state) =>
          val session = SessionUtil.setTdSession(
            // stateをセッションから削除
            SessionUtil.remove(request.session, Constants.SessionName.TD_STATE),
            // セッションに色々設定
            state)
          Redirect(routes.HomeController.app)
            .withSession(session)
        case Left(e) =>
          // エラー
          Redirect(routes.HomeController.index)
            .flashing("danger" -> e.toString)
            .withNewSession
      }.recover {
        case ex: Throwable =>
          // 例外
          LogUtil.errorEx(ex)
          Redirect(routes.HomeController.index)
            .flashing("danger" -> ex.toString)
            .withNewSession
      }
      f
    })
  }

  /**
    * アカウント情報を取得する
    *
    * @return
    */
  def getAccountInfo() = Action.async { implicit request: MessagesRequest[AnyContent] =>
    Logging("ToodledoController.getAccountInfo", {
      val stateOpt = SessionUtil.getTdSessionState(request.session)
      stateOpt match {
        case Some(state) =>
          val et = tdService.getAccountInfo(state)
          val f = et.value.map {
            case Right((accountInfo, state)) =>
              Logger.info(s"accountInfo = $accountInfo, state = $state")

              val session = SessionUtil.setTdSession(
                request.session,
                // セッションに色々設定
                state)

              Ok(Json.toJson(accountInfo.toShared))
                .withSession(session)
            case Left(e) =>
              Logger.error(e.toString)
              Redirect(routes.HomeController.index)
                .flashing("danger" -> e.toString)
                .withNewSession
          }.recover {
            case ex: Throwable =>
              // 例外
              LogUtil.errorEx(ex)
              Redirect(routes.HomeController.index)
                .flashing("danger" -> ex.toString)
                .withNewSession
          }
          f
        case _ =>
          Logger.error("ログインしてない")
          Future.successful {
            Redirect(routes.HomeController.index)
              .flashing("danger" -> Messages("toodledo.not_login"))
              .withNewSession
          }
      }
    })
  }

  /**
    * タスク一覧を取得する
    *
    * @return
    */
  def getTasks() = Action.async { implicit request: MessagesRequest[AnyContent] =>
    Logging("ToodledoController.getTasks", {
      val num = 1000

      val stateOpt = SessionUtil.getTdSessionState(request.session)
      stateOpt match {
        case Some(state) =>
          val et = tdService.getTasks(state, 0, num, 0)
          val f = et.value.map {
            case Right((tasks, num, total, state)) =>
              Logger.info(s"num = $num, total = $total, tasks = $tasks, state = $state")

              val session = SessionUtil.setTdSession(
                request.session,
                // セッションに色々設定
                state)

              Ok(Json.toJson(tasks.map(t => t.toShared)))
                .withSession(session)
            case Left(e) =>
              Logger.error(e.toString)
              Redirect(routes.HomeController.index)
                .flashing("danger" -> e.toString)
                .withNewSession
          }.recover {
            case ex: Throwable =>
              // 例外
              LogUtil.errorEx(ex)
              Redirect(routes.HomeController.index)
                .flashing("danger" -> ex.toString)
                .withNewSession
          }
          f
        case _ =>
          Logger.error("ログインしてない")
          Future.successful {
            Redirect(routes.HomeController.index)
              .flashing("danger" -> Messages("toodledo.not_login"))
              .withNewSession
          }
      }
    })
  }
}
