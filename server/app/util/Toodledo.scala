package util

import cats.data._
import cats.implicits._

import com.binbo_kodakusan.mtask.models.td
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.{JsDefined, JsUndefined}
import play.api.libs.ws.{WSAuthScheme, WSClient}

import scala.concurrent.{ExecutionContext, Future}

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
  def checkState[T](oldState: String, state: String, error: Option[String])
                   (implicit messages: Messages): EitherT[Future, AppError, Unit] = {
    try {
      Logger.info("START: Toodledo.checkState")
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
    }
    finally {
      Logger.info("END: Toodledo.checkState")
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
  def getAccessToken(url: String, code: String, client_id: String,
                     secret: String, device: String, at_token_took: Option[Long])
                    (implicit ex: ExecutionContext, ws: WSClient)
    : EitherT[Future, AppError, td.SessionState] = {
    try {
      Logger.info("START: Toodledo.getAccessToken")
      val wsreq = WSUtil.url(url)
        .withAuth(client_id, secret, WSAuthScheme.BASIC)
      Logger.info(s"request to ${wsreq.url}")

      EitherT[Future, AppError, td.SessionState] {
        wsreq.post(Map(
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
          val at_token_took2 = at_token_took match {
            case Some(v) => v
            case None => System.currentTimeMillis
          }
          Logger.info(s"token = $token, expires_in = $expires_in, token_type = $token_type, scope = $scope, refresh_token = $refresh_token, at_token_took: $at_token_took2")

          Right(td.SessionState(token, refresh_token, expires_in, at_token_took2))
        }.recover {
          case ex => {
            LogUtil.errorEx(ex, "ERROR(getAccessToken)")
            Left(AppError.Exception(ex))
          }
        }
      }
    }
    finally {
      Logger.info("END: Toodledo.getAccessToken")
    }
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
  def refreshAccessToken[T](url: String, refresh_token: String, client_id: String,
                            secret: String, device: String, at_token_took: Option[Long])
                           (implicit ws: WSClient, ec: ExecutionContext)
    : EitherT[Future, AppError, td.SessionState] = {
    try {
      Logger.info("START: Toodledo.refreshAccessToken")
      val wsreq = WSUtil.url(url)
        .withAuth(client_id, secret, WSAuthScheme.BASIC)
      Logger.info(s"request to ${wsreq.url}")

      EitherT[Future, AppError, td.SessionState] {
        wsreq.post(Map(
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

          Right(td.SessionState(token, refresh_token, expires_in, System.currentTimeMillis))
        }.recover {
          case ex => {
            // responce at maintenance
            // {"errorCode":4,"errorDesc":"The API is offline for maintenance."}
            LogUtil.errorEx(ex, "ERROR(refreshAccessToken)")
            Left(AppError.Exception(ex))
          }
        }
      }
    }
    finally {
      Logger.info("END: Toodledo.refreshAccessToken")
    }
  }

  def getTasks[T](url: String, start: Int, num: Int, tdState: td.SessionState)
                 (implicit ws: WSClient, ec: ExecutionContext)
    : EitherT[Future, AppError, (Some[Seq[td.Task]], Int, Int, td.SessionState)] = {
    try {
      Logger.info("START: Toodledo.getTasks")
      val wsreq = WSUtil.url(url)
        .addQueryStringParameters(
          "access_token" -> tdState.token,
          "start" -> start.toString,
          "num" -> num.toString,
          "fields" -> "folder,tag,star,priority,note"
        )
      Logger.info(s"request to ${wsreq.url}")

      EitherT[Future, AppError, (Some[Seq[td.Task]], Int, Int, td.SessionState)] {
        wsreq.get.map { response =>
          Logger.info(response.body)

          response.json \ "errorCode" match {
            case JsUndefined() =>
              // errorCodeが存在しないので正常
              // JSONをパースしてタスクを返す
              val num = (response.json \ 0 \ "num").as[Int]
              val total = (response.json \ 0 \ "total").as[Int]
              val tasks = for (i <- 1 to num)
                yield (response.json \ i).as[td.Task]
              Right((Some(tasks), num, total, tdState))
            case JsDefined(v) =>
              // errorCodeが設定されているのでエラー
              Left(AppError.Json(response.json))
          }
        }
      }
    }
    finally {
      Logger.info("END: Toodledo.getTasks")
    }
  }
}
