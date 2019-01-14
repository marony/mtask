package com.binbo_kodakusan.mtask.controllers

import javax.inject._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._
import com.binbo_kodakusan.mtask.Constants
import play.api.libs.json.{JsDefined, JsUndefined}
import play.api.{Configuration, Logger}
import util.{LogUtil, SessionUtil, WSUtil}

import scala.util.{Failure, Success, Try}
import com.binbo_kodakusan.mtask.models._
import play.api.i18n.{I18nSupport, Messages}

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
    * タスク一覧を取得する
    *
    * @return
    */
  def getTasks() = Action { implicit request =>
    Logger.info(s"Toodledo::getTasks called")

    val url = config.get[String]("toodledo.get_task.url")
    val tdStateOp = SessionUtil.getTdSessionState(request.session)
    tdStateOp match {
      case Some(tdState) =>
        ToodledoController.getTasks(url, tdState).map {
          case (tasks, tdState2) => {
            // TODO: タスク一覧表示
            val session = SessionUtil.setTdSession(
              request.session, tdState2)
            Logger.info(tasks.toString)
            Redirect(routes.Application.app)
              .withSession(session)
          }
        }.recover {
          case ex => {
            // responce at maintenance
            // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
            LogUtil.errorEx(ex, "ERROR(Toodledo.getTasks)")
            Redirect(routes.Application.index)
              .flashing("danger" -> ex.toString)
              .withNewSession
          }
        }.get
      case None =>
        val ex = new Exception(Messages("toodledo.not_login"))
        LogUtil.errorEx(ex, "ERROR(Toodledo.getTasks)")
        Redirect(routes.Application.index)
          .flashing("danger" -> ex.toString)
          .withNewSession
    }
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
  def callback(code: String, state: String, error: Option[String]) = Action { implicit request =>
    Logger.info(s"Toodledo::callback called: code = $code, state = $state, error = $error")

    ToodledoController.checkState(state, error).flatMap { case _ => {
      // codeからアクセストークンを取得
      ToodledoController.getAccessToken(code).map { tdState =>
        val session = SessionUtil.setTdSession(
          // stateをセッションから削除
          SessionUtil.remove(request.session, Constants.SessionName.TD_STATE),
          // セッションに色々設定
          tdState)
        Redirect(routes.Application.app)
          .withSession(session)
      }
    }}.recover {
      case ex => {
        LogUtil.errorEx(ex, "ERROR(Toodledo.callback)")
        // フラッシュでエラー内容表示
        Redirect(routes.Application.index)
          .flashing("danger" -> ex.toString)
          .withNewSession
      }
    }.get
  }
}

object ToodledoController {
  /**
    * コールバックの内容をチェックしてエラーを返す
    *
    * @param config
    * @param ws
    * @param request
    * @param state
    * @param error
    * @param ec
    * @tparam T
    * @return
    */
  def checkState[T](state: String, error: Option[String])
                   (implicit request: Request[T], messages: Messages): Try[Unit] = {
    error.map { e =>
      // Toodledoからエラーを返された
      // error is Some(e)
      Failure(new Exception(e))
    }.getOrElse {
      request.session.get(Constants.SessionName.TD_STATE).map { myState =>
        if (myState == state) {
          Success(())
        } else {
          Failure(new Exception(Messages("toodledo.invalid_state")))
        }
      }.getOrElse {
        // session is null
        Failure(new Exception(Messages("toodledo.not_login")))
      }
    }
  }

  /**
    * アクセストークン(とリフレッシュトークン)を取得
    *
    * @param config
    * @param ws
    * @param request
    * @param code
    * @param ec
    * @tparam T
    * @return
    */
  def getAccessToken[T](code: String)
                       (implicit config: Configuration, ws: WSClient, request: Request[T],
                        ec: ExecutionContext): Try[td.SessionState] = {
    val url = config.get[String]("toodledo.token.url")
    val client_id = config.get[String]("toodledo.client_id")
    val secret = config.get[String]("toodledo.secret")
    val device = request.headers("User-Agent")

    val wsreq = WSUtil.url(url)
      .withAuth(client_id, secret, WSAuthScheme.BASIC)
    Logger.info(s"request to ${wsreq.url}")

    val f = wsreq.post(Map(
      "grant_type" -> "authorization_code",
      "code" -> code,
      "device" -> device
    )).map { response =>
      Logger.info(response.body)

      // レスポンスからアクセストークンを取得
      val token = (response.json \ "access_token").as[String]
      val expires_in = (response.json \ "expires_in").as[Int]
      val token_type = (response.json \ "token_type").as[String]
      val scope = (response.json \ "scope").as[String]
      val refresh_token = (response.json \ "refresh_token").as[String]
      val at_token_took = request.session.get(Constants.SessionName.TD_AT_TOKEN_TOOK) match {
        case Some(v) => v.toLong
        case None => System.currentTimeMillis
      }
      Logger.info(s"token = $token, expires_in = $expires_in, token_type = $token_type, scope = $scope, refresh_token = $refresh_token")

      Success(td.SessionState(token, refresh_token, expires_in, at_token_took))
    }.recover {
      case ex => {
        // responce at maintenance
        // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
        LogUtil.errorEx(ex, "ERROR(Toodledo.getAccessToken)")
        Failure(ex)
      }
    }
    Await.ready(f, Duration.Inf)
    f.value.get.get
  }

  /**
    * リフレッシュトークンから新しいアクセストークンを取得する
    *
    * @param config
    * @param ws
    * @param request
    * @param ec
    * @tparam T
    * @return
    */
  def refreshAccessToken[T]()
                           (implicit config: Configuration, ws: WSClient, request: Request[T],
                            ec: ExecutionContext): Try[td.SessionState] = {
    val url = config.get[String]("toodledo.token.url")
    val client_id = config.get[String]("toodledo.client_id")
    val secret = config.get[String]("toodledo.secret")
    val refresh_token = request.session.get(Constants.SessionName.TD_REFRESH_TOKEN).get
    val device = request.headers("User-Agent")

    val wsreq = WSUtil.url(url)
      .withAuth(client_id, secret, WSAuthScheme.BASIC)
    Logger.info(s"request to ${wsreq.url}")

    val f = wsreq.post(Map(
      "grant_type" -> "refresh_token",
      "refresh_token" -> refresh_token,
      "device" -> device
    )).map { response =>
      Logger.info(response.body)

      // レスポンスからアクセストークンを取得
      val token = (response.json \ "access_token").as[String]
      val expires_in = (response.json \ "expires_in").as[Int]
      val token_type = (response.json \ "token_type").as[String]
      val scope = (response.json \ "scope").as[String]
      val refresh_token = (response.json \ "refresh_token").as[String]
      Logger.info(s"token = $token, expires_in = $expires_in, token_type = $token_type, scope = $scope, refresh_token = $refresh_token")

      Success(td.SessionState(token, refresh_token, expires_in, System.currentTimeMillis))
    }.recover {
      case ex => {
        // responce at maintenance
        // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
        LogUtil.errorEx(ex, "ERROR(refreshAccessToken)")
        Failure(ex)
      }
    }
    Await.ready(f, Duration.Inf)
    f.value.get.get
  }

  def getTasks[T](url: String, tdState: td.SessionState)
                           (implicit config: Configuration, ws: WSClient, request: Request[T],
                            ec: ExecutionContext): Try[(Option[Seq[td.Task]], td.SessionState)] = {

    // TODO: 1000件以上だったら繰り返し呼び出し
    // TODO: アクセストークンが切れてたらリフレッシュ

    val wsreq = WSUtil.url(url)
      .addQueryStringParameters(
        "access_token" -> tdState.token,
        "start" -> "0",
        "num" -> "10", // TODO: 1000件以上取得できるように
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
}
