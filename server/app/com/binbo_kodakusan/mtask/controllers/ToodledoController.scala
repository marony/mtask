package com.binbo_kodakusan.mtask.controllers

import cats.data._
import cats.implicits._
import javax.inject._
import com.binbo_kodakusan.mtask.Constants
import com.binbo_kodakusan.mtask.models.{SessionState, TdAccountInfo, TdDeletedTask, TdTask}
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc._
import play.api.{Configuration, Logger}
import com.binbo_kodakusan.mtask.services.{ToodledoApi, ToodledoService, UserService}
import com.binbo_kodakusan.mtask.util._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ToodledoController @Inject()
  (mcc: MessagesControllerComponents,
   usersService: UserService,
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
  def callback(code: String, state: String, error: Option[String]) = Action { implicit request: MessagesRequest[AnyContent] =>
    Logging("ToodledoController.callback", {
      Logger.info(s"ToodledoController::callback called: code = $code, state = $state, error = $error")

      val oldState = request.session.get(Constants.SessionName.TD_STATE)

      val clientId = config.get[String]("toodledo.client_id")
      val secret = config.get[String]("toodledo.secret")
      val device = request.headers("User-Agent")
      // まだat_token_tookはない可能性がある
      val atTokenTook = request.session.get(Constants.SessionName.TD_AT_TOKEN_TOOK).map(_.toLong)

      (tdService.checkState(oldState, state, error),
        tdService.getAccessToken(code, clientId, secret, device, atTokenTook)) match {
        case (Right(_), Right(state)) =>
          // アカウント情報の取得
          tdService.getAccountInfo(state).map { case (accountInfo, state) =>
            Logger.info(accountInfo.toString)
            usersService.upsertAccountInfo(accountInfo, Some(0), Some(state))
          }
          // うまくいったらReactのアプリを表示
          val session = SessionUtil.setTdSession(
            // stateをセッションから削除
            SessionUtil.remove(request.session, Constants.SessionName.TD_STATE),
            // セッションに色々設定
            state)
          Redirect(routes.HomeController.app)
            .withSession(session)
        case (Left(l), _) =>
          Redirect(routes.HomeController.index)
            .flashing("danger" -> l.toString)
            .withNewSession
        case (_, Left(l)) =>
          Redirect(routes.HomeController.index)
            .flashing("danger" -> l.toString)
            .withNewSession
      }
    })
  }

  /**
    * アカウント情報を取得する
    *
    * @return
    */
  def getAccountInfo() = Action { implicit request: MessagesRequest[AnyContent] =>
    Logging("ToodledoController.getAccountInfo", {
      // タスクを取得する
      val state = SessionUtil.getTdSessionState(request.session)
      val r: Either[AppError, (TdAccountInfo, SessionState)] = tdService.getAccountInfo(state.get)

      r match {
        case Right(r) =>
          val accountInfo = r._1
          val state = r._2
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
      }
    })
  }

  /**
    * タスク一覧を取得する
    * 削除済みのタスクは取り除く
    *
    * @return
    */
  def getTasks() = Action { implicit request: MessagesRequest[AnyContent] =>
    Logging("ToodledoController.getTasks", {
      // タスクを取得する
      val num = 1000
      val state = SessionUtil.getTdSessionState(request.session)

      val r1 = tdService.getTasks(state.get, 0, num, 0)
      r1 match {
        case Right((tasks, num, total, state1)) =>
          Logger.info(s"num = $num, total = $total, tasks = $tasks, state = $state1")
          val r2 = tdService.getDeletedTasks(state1)
          r2 match {
            case Right((deletedTasks, state2)) =>
              Logger.info(s"deletedTasks = $deletedTasks, state = $state2")
              val session = SessionUtil.setTdSession(
                request.session,
                // セッションに色々設定
                state2)

              // 削除済みのタスクを取り除く
              Ok(Json.toJson(tasks.filter(t => deletedTasks.forall(d => t.id != d.id)).map(t => t.toShared())))
                .withSession(session)
            case Left(e) =>
              Logger.error(e.toString)
              Redirect(routes.HomeController.index)
                .flashing("danger" -> e.toString)
                .withNewSession
          }
        case Left(e) =>
          Logger.error(e.toString)
          Redirect(routes.HomeController.index)
            .flashing("danger" -> e.toString)
            .withNewSession
      }
    })
  }
}
