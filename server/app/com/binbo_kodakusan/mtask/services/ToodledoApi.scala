package com.binbo_kodakusan.mtask.services

import cats.data._
import com.binbo_kodakusan.mtask.models.{SessionState, TdAccountInfo, TdDeletedTask, TdTask}
import javax.inject._
import play.api.{Configuration, Logger}
import play.api.i18n.Messages
import play.api.libs.ws.{WSAuthScheme, WSClient}
import com.binbo_kodakusan.mtask.util._
import play.api.mvc.{MessagesRequest}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class ToodledoApi @Inject()
  (implicit config: Configuration, ex: ExecutionContext, ws: WSClient) {

  /**
    * コールバックの内容をチェックしてエラーを返す
    *
    * @param oldState
    * @param state
    * @param error
    * @param messages
    * @return
    */
  def checkState(oldState: String, state: String, error: Option[String])
                   (implicit messages: Messages)
                   : EitherT[Future, AppError, Unit] = {
    Logging("Toodledo.checkState", {
      error.map { e =>
        // Toodledoからエラーを返された
        EitherT[Future, AppError, Unit](Future.successful(Left(AppError.Error(e))))
      }.getOrElse {
        if (oldState == state) {
          EitherT[Future, AppError, Unit](Future.successful(Right(())))
        } else {
          EitherT[Future, AppError, Unit](Future.successful(Left(AppError.Error(Messages("toodledo.invalid_state")))))
        }
      }
    })
  }

  /**
    * アクセストークン(とリフレッシュトークン)を取得
    *
    * @param code
    * @param clientId
    * @param secret
    * @param device
    * @param atTokenTook
    * @return
    */
  def getAccessToken(code: String, clientId: String,
                     secret: String, device: String, atTokenTook: Option[Long])
    : Future[SessionState] = {

    Logging("Toodledo.getAccessToken", {
      val url = config.get[String]("toodledo.token.url")
      val wsreq = WSUtil.url(url)
        .withAuth(clientId, secret, WSAuthScheme.BASIC)
      Logger.info(s"request to ${wsreq.url}")

      wsreq.post(Map(
        "grant_type" -> "authorization_code",
        "code" -> code,
        "device" -> device
      )).map { response => {
        Logger.info(response.body)

        // レスポンスからアクセストークンを取得
        val token = (response.json \ "access_token").as[String]
        val expiresIn = (response.json \ "expires_in").as[Int]
        val tokenType = (response.json \ "token_type").as[String]
        val scope = (response.json \ "scope").as[String]
        val refreshToken = (response.json \ "refresh_token").as[String]
        val atTokenTook2 = atTokenTook match {
          case Some(v) => v
          case None => System.currentTimeMillis
        }
        Logger.info(s"token = $token, expires_in = $expiresIn, token_type = $tokenType, scope = $scope, refresh_token = $refreshToken, at_token_took: $atTokenTook2")

        SessionState(token, refreshToken, expiresIn, atTokenTook2)
      }}
    })
  }

  /**
    * リフレッシュトークンから新しいアクセストークンを取得する
    *
    * @param oldState
    * @param request
    * @tparam T
    * @return
    */
  def refreshAccessToken[T](oldState: SessionState)
                           (implicit request: MessagesRequest[T])
    : Future[SessionState] = {

    Logging("Toodledo.refreshAccessToken", {
      val url = config.get[String]("toodledo.token.url")
      val clientId = config.get[String]("toodledo.client_id")
      val secret = config.get[String]("toodledo.secret")
      val device = request.headers("User-Agent")

      val wsreq = WSUtil.url(url)
        .withAuth(clientId, secret, WSAuthScheme.BASIC)
      Logger.info(s"request to ${wsreq.url}")

      wsreq.post(Map(
        "grant_type" -> "refresh_token",
        "refresh_token" -> oldState.refreshToken,
        "device" -> device
      )).map { response =>
        Logger.info(response.body)

        // レスポンスからアクセストークンを取得
        val token = (response.json \ "access_token").as[String]
        val expiresIn = (response.json \ "expires_in").as[Int]
        val tokenType = (response.json \ "token_type").as[String]
        val scope = (response.json \ "scope").as[String]
        val refreshToken = (response.json \ "refresh_token").as[String]
        Logger.info(s"token = ${oldState.token} -> $token, expires_in = $expiresIn, token_type = $tokenType, scope = $scope, refresh_token = $refreshToken")

        SessionState(token, refreshToken, expiresIn, System.currentTimeMillis)
      }
    })
  }

  /**
    * アカウント情報を取得する
    *
    * @param url
    * @param state
    * @param retry
    * @param request
    * @tparam T
    * @return
    */
  def getAccountInfo[T](state: SessionState, retry: Boolean = false)
                       (implicit request: MessagesRequest[T])
    : Future[(TdAccountInfo, SessionState)] = {

    Logging("Toodledo.getAccountInfo", {
      val url = config.get[String]("toodledo.account_info.url")
      val wsreq = WSUtil.url(url)
        .addQueryStringParameters(
          "access_token" -> state.token
        )
      Logger.info(s"request to ${wsreq.url}")

      wsreq.get.map { response =>
        Logger.info(response.body)

        ((response.json \ "errorCode").asOpt[String], (response.json \ "errorDesc").asOpt[String]) match {
          case (Some(errorCode), Some(errorDesc)) =>
            // errorCodeが設定されているのでエラー
            // ex) アクセストークンが切れている場合
            // {"errorCode":2,"errorDesc":"Unauthorized","errors":[{"status":"2","message":"Unauthorized"}]}
            // ex) メンテナンス中の場合
            // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
            if (!retry && errorCode == "2") {
              // トークンの再取得
              val rr = for {
                newState <- refreshAccessToken(state)
                r <- getAccountInfo(newState, true)
              } yield {
                r
              }
              // FIXME: 同期で待ちたくない
              Await.result(rr, Duration.Inf)
            } else {
              // その他Toodledoのエラー
              throw new ToodledoException(errorCode, errorDesc, response.json)
            }
          case _ =>
            // 正常
            (response.json.as[TdAccountInfo], state)
        }
      }
    })
  }

  /**
    * タスクを取得する
    *
    * @param start
    * @param num
    * @param state
    * @param retry
    * @param request
    * @tparam T
    * @return
    */
  def getTasks[T](start: Int, num: Int, state: SessionState, retry: Boolean = false)
                 (implicit request: MessagesRequest[T])
    : Future[(Seq[TdTask], Int, Int, SessionState)] = {

    Logging("Toodledo.getTasks", {
      val url = config.get[String]("toodledo.get_task.url")
      val wsreq = WSUtil.url(url)
        .addQueryStringParameters(
          "access_token" -> state.token,
          "start" -> start.toString,
          "num" -> num.toString,
          // default: id, title, modified, completed
          "fields" -> "folder,context,goal,location,tag,startdate,duedate,duedatemod,starttime,duetime,remind,repeat,status,star,priority,length,timer,added,note,parent,children,order,meta,previous,attachment,shared,addedby,via,attachments"
        )
      Logger.info(s"request to ${wsreq.url}")

      wsreq.get.map { response =>
        Logger.info(response.body)

        ((response.json \ "errorCode").asOpt[String], (response.json \ "errorDesc").asOpt[String]) match {
          case (Some(errorCode), Some(errorDesc)) =>
            // errorCodeが設定されているのでエラー
            // ex) アクセストークンが切れている場合
            // {"errorCode":2,"errorDesc":"Unauthorized","errors":[{"status":"2","message":"Unauthorized"}]}
            // ex) メンテナンス中の場合
            // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
            if (!retry && errorCode == "2") {
              // トークンの再取得
              val rr = for {
                newState <- refreshAccessToken(state)
                r <- getTasks(start, num, newState, true)
              } yield {
                r
              }
              // FIXME: 同期で待ちたくない
              Await.result(rr, Duration.Inf)
            } else {
              // その他Toodledoのエラー
              throw new ToodledoException(errorCode, errorDesc, response.json)
            }
          case _ =>
            // 正常
            val num = (response.json \ 0 \ "num").as[Int]
            val total = (response.json \ 0 \ "total").as[Int]
            val tasks = for (i <- 1 to num)
              yield (response.json \ i).as[TdTask]
            (tasks, num, total, state)
        }
      }
    })
  }

  /**
    * 削除されたタスクを取得する
    *
    * @param url
    * @param afterOpt
    * @param state
    * @param request
    * @tparam T
    * @return
    */
  def getDeletedTasks[T](afterOpt: Option[Int], state: SessionState, retry: Boolean = false)
                        (implicit request: MessagesRequest[T])
    : Future[(Seq[TdDeletedTask], SessionState)] = {

    Logging("Toodledo.getDeletedTasks", {
      val url = config.get[String]("toodledo.deleted_task.url")
      var wsreq = WSUtil.url(url)
        .addQueryStringParameters(
          "access_token" -> state.token,
          "after" -> (if (afterOpt.isEmpty) "0" else afterOpt.get.toString),
        )
      Logger.info(s"request to ${wsreq.url}")

      wsreq.get.map { response =>
        Logger.info(response.body)

        ((response.json \ "errorCode").asOpt[String], (response.json \ "errorDesc").asOpt[String]) match {
          case (Some(errorCode), Some(errorDesc)) =>
            // errorCodeが設定されているのでエラー
            // ex) アクセストークンが切れている場合
            // {"errorCode":2,"errorDesc":"Unauthorized","errors":[{"status":"2","message":"Unauthorized"}]}
            // ex) メンテナンス中の場合
            // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
            if (!retry && errorCode == "2") {
              // トークンの再取得
              val rr = for {
                newState <- refreshAccessToken(state)
                r <- getDeletedTasks(afterOpt, newState, true)
              } yield {
                r
              }
              // FIXME: 同期で待ちたくない
              Await.result(rr, Duration.Inf)
            } else {
              // その他Toodledoのエラー
              throw new ToodledoException(errorCode, errorDesc, response.json)
            }
          case _ =>
            // 正常
            val num = (response.json \ 0 \ "num").as[Int]
            val deletedTasks = for (i <- 1 to num)
              yield (response.json \ i).as[TdDeletedTask]
            (deletedTasks, state)
        }
      }
    })
  }
}
