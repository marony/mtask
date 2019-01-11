package com.binbo_kodakusan.mtask.controllers

import javax.inject._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._
import com.binbo_kodakusan.mtask.Constants
import play.api.{Configuration, Logger}

import scala.util.{Failure, Success}

@Singleton
class Toodledo @Inject()
  (config: Configuration, ws: WSClient, cc: ControllerComponents)
  (implicit ec: ExecutionContext, webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

  /**
    * Toodledoの認証
    * Toodledoのログインページにリダイレクトする
    *
    * @return
    */
  def authorize() = Action {
    Logger.info(s"Toodledo::authorize called")

    val url = config.get[String]("toodledo.authorize.url")
    val client_id = config.get[String]("toodledo.client_id")
    val state = java.util.UUID.randomUUID.toString
    val scope = config.get[String]("toodledo.authorize.scope")

    Redirect(url, Map(
      "response_type" -> Seq("code"),
      "client_id" -> Seq(client_id),
      "state" -> Seq(state),
      "scope" -> Seq(scope)
    ))
      .withSession(Constants.SessionName.TD_STATE -> state)
  }

  /**
    * コールバックの内容をチェックしてエラーを返す
    *
    * @param request
    * @param state
    * @param error
    * @tparam T
    * @return
    */
  private[this] def checkState[T](request: Request[T], state: String, error: Option[String]): Option[String] = {
    error.map { e =>
      // error is Some(e)
      Some(e)
    }.getOrElse {
      request.session.get("td_state").map { myState =>
        if (myState == state) {
          None
        } else {
          // TODO: メッセージ定義
          Some("invalid state")
        }
      }.getOrElse {
        // session is null
        // TODO: メッセージ定義
        Some("invlaid user")
      }
    }
  }

  /**
    * アクセストークン(とリフレッシュトークン)を取得
    *
    * @param code
    * @return
    */
  private[this] def getAccessToken(code: String): Option[(String, String)] = {
    val url = config.get[String]("toodledo.token.url")
    val client_id = config.get[String]("toodledo.client_id")
    val secret = config.get[String]("toodledo.secret")

    val request = ws.url(url)
      .withAuth(client_id, secret, WSAuthScheme.BASIC)
    Logger.info(s"request to ${request.url}")

    var r: Option[(String, String)] = None
    val f = request.post(Map(
      "grant_type" -> "authorization_code",
      "code" -> code
    ))
    f onComplete {
      case Success(response) => {
        Logger.info(response.body)

        // レスポンスからアクセストークンを取得
        val token = (response.json \ "access_token").as[String]
        val expires_in = (response.json \ "expires_in").as[Int]
        val token_type = (response.json \ "token_type").as[String]
        val scope = (response.json \ "scope").as[String]
        val refresh_token = (response.json \ "refresh_token").as[String]
        Logger.info(s"token: token = $token, expires_in = $expires_in, token_type = $token_type, scope = $scope, refresh_token = $refresh_token")

        r = Some((token, refresh_token))
      }
      case Failure(ex) => {
        Logger.error(s"ERROR1: $ex")
      }
    }
    Await.ready(f, Duration.Inf)
    r
  }

  /**
    * Toodledoからの認証コールバック
    * アクセストークンを取得する
    *
    * @return
    */
  def callback(code: String, state: String, error: Option[String]) =
    Action { implicit request =>
    Logger.info(s"Toodledo::callback called: code = $code, state = $state, error = $error")

    checkState(request, state, error) match {
      case Some(e) => {
        Logger.error(e)
        // TODO: フラッシュでエラー内容表示
        Redirect(routes.Application.index)
          .withNewSession
      }
      case None => {
        // 成功
        // codeからアクセストークンを取得
        getAccessToken(code) match {
          case Some((token, refresh_token)) => {
            Redirect(routes.Application.app)
              .withSession(
                Constants.SessionName.TD_TOKEN -> token,
                Constants.SessionName.TD_REFRESH_TOKEN -> refresh_token
              )
          }
          case None => {
            Logger.error("ERROR2: ")
            // TODO: フラッシュでエラー内容表示
            Redirect(routes.Application.index)
              .withNewSession
          }
        }
      }
    }
  }
}
