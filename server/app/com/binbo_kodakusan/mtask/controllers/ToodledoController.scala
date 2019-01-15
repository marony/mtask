package com.binbo_kodakusan.mtask.controllers

import cats.data._
import cats.implicits._
import com.binbo_kodakusan.mtask.Constants
import com.binbo_kodakusan.mtask.models.td
import javax.inject._
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.ws._
import play.api.mvc._
import play.api.{Configuration, Logger}
import util._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

//import cats.data._
//import cats.implicits._

@Singleton
class ToodledoController @Inject()
  (cc: ControllerComponents)
  (implicit ec: ExecutionContext, config: Configuration, ws: WSClient)
    extends AbstractController(cc)
    with I18nSupport {

  /**
    * Toodledoの認証
    * Toodledoのログインページにリダイレクトする
    *
    * @return
    */
  def authorize() = Action { implicit request =>
    Logger.info(s"Toodledo::authorize called")

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
  def callback(code: String, state: String, error: Option[String]) = Action.async { implicit request =>
    Logger.info(s"Toodledo::callback called: code = $code, state = $state, error = $error")

    val url = config.get[String]("toodledo.token.url")
    val client_id = config.get[String]("toodledo.client_id")
    val secret = config.get[String]("toodledo.secret")
    val device = request.headers("User-Agent")
    val at_token_took = request.session.get(Constants.SessionName.TD_AT_TOKEN_TOOK).map(_.toLong)
    val oldStateOpt = EitherT[Future, AppError, String] {
      request.session.get(Constants.SessionName.TD_STATE)
        match { case Some(v) => Future.successful(Right(v)) case None => Future.successful(Left(AppError.NoError())) }
    }

    // EitherT[Future, AppError, Result]の正常ロジック
    val et: EitherT[Future, AppError, Result] = for {
      oldState <- oldStateOpt
      r1 <- Toodledo.checkState(oldState, state, error)
      tdState <- Toodledo.getAccessToken(url, code, client_id, secret, device, at_token_took)
    } yield {
      val session = SessionUtil.setTdSession(
        // stateをセッションから削除
        SessionUtil.remove(request.session, Constants.SessionName.TD_STATE),
        // セッションに色々設定
        tdState)
      Redirect(routes.HomeController.app)
        .withSession(session)
    }
    // Future[Result](EitherT.value)のエラー系ロジック
    EitherTUtil.eitherT2Error(et, v => {
      Logger.error(v.toString)
      Redirect(routes.HomeController.index)
        .flashing("danger" -> v.toString)
        .withNewSession
    }, ex => {
      LogUtil.errorEx(ex)
      Redirect(routes.HomeController.index)
        .flashing("danger" -> ex.toString)
        .withNewSession
    })
  }

  /**
    * タスク一覧を取得する
    *
    * @return
    */
  def getTasks() = Action.async { implicit request =>
    Logger.info(s"Toodledo::getTasks called")

    val url = config.get[String]("toodledo.get_task.url")
    val start = 0
    val num = 10 // TODO: 1000件にする

    val oldStateOpt = EitherT[Future, AppError, td.SessionState] {
      SessionUtil.getTdSessionState(request.session)
        match { case Some(v) => Future.successful(Right(v)) case None => Future.successful(Left(AppError.NoError())) }
    }
    val et: EitherT[Future, AppError, Result] = for {
      tdState <- oldStateOpt
      tasksAndState <- Toodledo.getTasks(url, start, num, tdState)
    } yield {
      val (Some(tasks), num2, total, tdState2) = tasksAndState
      // TODO: タスクを返す
      Logger.info(tasks.toString)

      val session = SessionUtil.setTdSession(
        request.session,
        // セッションに色々設定
        tdState2)

      Redirect(routes.HomeController.app)
        .withSession(session)
    }
    // Future[Result](EitherT.value)のエラー系ロジック
    EitherTUtil.eitherT2Error(et, v => {
      Logger.error(v.toString)
      Redirect(routes.HomeController.index)
        .flashing("danger" -> v.toString)
        .withNewSession
    }, ex => {
      LogUtil.errorEx(ex)
      Redirect(routes.HomeController.index)
        .flashing("danger" -> ex.toString)
        .withNewSession
    })
/*
    }.getOrElse {
      val ex = new Exception(Messages("toodledo.not_login"))
      LogUtil.errorEx(ex, "ERROR(Toodledo.getTasks)")
      Redirect(routes.HomeController.index)
        .flashing("danger" -> ex.toString)
        .withNewSession
    }
*/
/*
    val tdStateOp = SessionUtil.getTdSessionState(request.session)
    tdStateOp match {
      case Some(tdState) =>
        ToodledoController.getTasks(url, tdState).map {
          case (tasks, tdState2) => {
            // TODO: タスク一覧表示
            val session = SessionUtil.setTdSession(
              request.session, tdState2)
            Logger.info(tasks.toString)
            Redirect(routes.HomeController.app)
              .withSession(session)
          }
        }.recover {
          case ex => {
            // responce at maintenance
            // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
            LogUtil.errorEx(ex, "ERROR(Toodledo.getTasks)")
            Redirect(routes.HomeController.index)
              .flashing("danger" -> ex.toString)
              .withNewSession
          }
        }.get
      case None =>
        val ex = new Exception(Messages("toodledo.not_login"))
        LogUtil.errorEx(ex, "ERROR(Toodledo.getTasks)")
        Redirect(routes.HomeController.index)
          .flashing("danger" -> ex.toString)
          .withNewSession
    }
*/
  }
}

object ToodledoController {
/*
  def getTasks[T](url: String, start: Int, num: Int, tdState: td.SessionState)
                 (implicit ws: WSClient, ec: ExecutionContext)
  : EitherT[Future, AppError, (Some[Seq[td.Task]], td.SessionState)] = {

    // TODO: 1000件以上だったら繰り返し呼び出し
    // TODO: アクセストークンが切れてたらリフレッシュ

    val wsreq = WSUtil.url(url)
      .addQueryStringParameters(
        "access_token" -> tdState.token,
        "start" -> start.toString,
        "num" -> num.toString,
        "fields" -> "folder,tag,star,priority,note"
      )
    Logger.info(s"request to ${wsreq.url}")

    val f = wsreq.get.map { response =>
      Logger.info(response.body)

      response.json \ "errorCode" match {
        case JsUndefined() => {
          // errorCodeが存在しないので正常
          // TODO: 実装
          // JSONをパースしてタスクを返す
          val num = (response.json \ 0 \ "num").as[Int]
          //          val total = (response.json \ 0 \ "total").as[Int]
          // TODO: num < totalならばstartを変えて再度呼び出し
          val tasks = for (i <- 1 to num)
            yield (response.json \ i).as[td.Task]
          Success((Some(tasks), tdState))
        }
        case JsDefined(v) => {
          // errorCodeが設定されているのでエラー
          if (v.as[Int] != 2) {
            Failure(new Exception(v.toString + ": " + (response.json \ "errorDesc").as[String]))
          } else {
            // {"errorCode":2,"errorDesc":"Unauthorized","errors":[{"status":"2","message":"Unauthorized"}]}
            // 2ならば認証エラーなのでアクセストークンを再要求
            // リフレッシュトークンからアクセストークンを取得して再起で自分を呼び出す
            ToodledoController.refreshAccessToken().flatMap { tdState =>
              getTasks(url, tdState)
            }
          }
        }
      }
    }
    Await.ready(f, Duration.Inf)
    f.value.get.get
  }
*/
}
