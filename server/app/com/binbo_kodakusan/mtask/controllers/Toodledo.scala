package com.binbo_kodakusan.mtask.controllers

import javax.inject._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._
import com.binbo_kodakusan.mtask.Constants
import play.api.libs.ws.ahc.AhcCurlRequestLogger
import play.api.{Configuration, Logger}
import util.{SSUtil, WSUtil}

import scala.util.{Failure, Success, Try}

@Singleton
class Toodledo @Inject()
  (cc: ControllerComponents)
  (implicit ec: ExecutionContext, config: Configuration, ws: WSClient, webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

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
    val session = SSUtil.add(request.session,
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

    // TODO: 1000件以上だったら繰り返し呼び出し
    // TODO: アクセストークンが切れてたらリフレッシュ

    val url = config.get[String]("toodledo.get_task.url")
    val token = request.session.get(Constants.SessionName.TD_TOKEN).get

    val wsreq = WSUtil.url(url)
      .addQueryStringParameters(
        "access_token" -> token,
        "start" -> "0",
        "num" -> "1000",
        "fields" -> "folder,tag,star,priority,note"
      )
    Logger.info(s"request to ${wsreq.url}")

    val f = wsreq.get.map { response =>
      Logger.info(response.body)
    }.recover {
      case ex => {
        // responce at maintenance
        // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
        Logger.error(s"ERROR1: $ex")
        Failure(ex)
      }
    }
    Await.ready(f, Duration.Inf)

    Redirect(routes.Application.app)
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

    Toodledo.checkState(state, error).flatMap { case _ => {
      // codeからアクセストークンを取得
      Toodledo.getAccessToken(code).map { case (token, refresh_token, expires_in) =>
        val session =
          SSUtil.add(
            // stateをセッションから削除
            SSUtil.remove(request.session, Constants.SessionName.TD_STATE),
            // セッションに色々設定
            Constants.SessionName.TD_TOKEN -> token,
            Constants.SessionName.TD_REFRESH_TOKEN -> refresh_token,
            Constants.SessionName.TD_EXPIRES_IN -> expires_in.toString,
            Constants.SessionName.TD_AT_TOKEN_TOOK -> System.currentTimeMillis.toString)
        Redirect(routes.Application.app)
          .withSession(session)
      }
    }}.recover {
      case ex => {
        Logger.error(ex.toString)
        // フラッシュでエラー内容表示
        Redirect(routes.Application.index)
          .flashing("danger" -> ex.toString)
          .withNewSession
      }
    }.get
  }
}

object Toodledo {
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
                   (implicit config: Configuration, ws: WSClient, request: Request[T],
                    ec: ExecutionContext): Try[Unit] = {
    error.map { e =>
      // Toodledoからエラーを返された
      // error is Some(e)
      Failure(new Exception(e))
    }.getOrElse {
      request.session.get(Constants.SessionName.TD_STATE).map { myState =>
        if (myState == state) {
          Success(())
        } else {
          // TODO: メッセージ定義
          Failure(new Exception("invalid state"))
        }
      }.getOrElse {
        // session is null
        // TODO: メッセージ定義
        Failure(new Exception("invalid user"))
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
                        ec: ExecutionContext): Try[(String, String, Int)] = {
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
      Logger.info(s"token = $token, expires_in = $expires_in, token_type = $token_type, scope = $scope, refresh_token = $refresh_token")

      Success((token, refresh_token, expires_in))
    }.recover {
      case ex => {
        // responce at maintenance
        // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
        Logger.error(s"ERROR1: $ex")
        Failure(ex)
      }
    }
    Await.ready(f, Duration.Inf)
    f.value.get.get
  }
}
