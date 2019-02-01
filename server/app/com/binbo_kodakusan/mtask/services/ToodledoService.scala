package com.binbo_kodakusan.mtask.services

import javax.inject._
import com.binbo_kodakusan.mtask.models.{SessionState, TdAccountInfo, TdDeletedTask, TdTask}
import play.api.{Configuration, Logger}
import play.api.libs.ws.WSClient
import play.api.mvc.MessagesRequest
import com.binbo_kodakusan.mtask.util.{AppError, Logging, SessionUtil}
import cats.data._
import cats.implicits._
import com.binbo_kodakusan.mtask.Constants
import com.binbo_kodakusan.mtask.controllers.routes
import play.api.i18n.Messages

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

@Singleton
class ToodledoService @Inject()
  (api: ToodledoApi, userService: UserService)
  (implicit config: Configuration, ws: WSClient, ec: ExecutionContext) {

  /**
    * Toodledoからのコールバックが正しいことをチェックする
    *
    * @param oldStateOpt
    * @param state
    * @param error
    * @param request
    * @tparam T
    * @return
    */
  private def checkState[T](oldStateOpt: Option[String], state: String, error: Option[String])
                   (implicit request: MessagesRequest[T])
    : EitherT[Future, AppError, Unit] = {

    Logging("ToodledoService.checkState", {
      oldStateOpt match {
        case Some(oldState) =>
          api.checkState(oldState, state, error)
        case _ => EitherT.leftT {
          // FIXME: メッセージ定義
          AppError.Error("FIXME: message")
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
    * @tparam T
    * @return
    */
  private def getAccessToken[T](code: String, clientId: String,
                        secret: String, device: String, atTokenTook: Option[Long])
    : EitherT[Future, AppError, SessionState] = {

    Logging("ToodledoService.getAccessToken", {
      val f = for {
        state <- api.getAccessToken(code, clientId, secret, device, atTokenTook)
      } yield {
        Logger.info(state.toString)
        Either.right[AppError, SessionState](state)
      }
      EitherT(f)
    })
  }

  /**
    * Toodledoを通してログイン
    *
    * @param oldState
    * @param state
    * @param error
    * @param code
    * @param clientId
    * @param secret
    * @param device
    * @param atTokenTook
    * @param request
    * @tparam T
    * @return
    */
  def login[T](oldState: Option[String], state: String, error: Option[String],
                             code: String, clientId: String,
                             secret: String, device: String, atTokenTook: Option[Long])
                               (implicit request: MessagesRequest[T])
    : EitherT[Future, AppError, SessionState] = {

    Logging("ToodledoService.login", {
      val et = for {
        _ <- checkState(oldState, state, error)
        state <- getAccessToken(code, clientId, secret, device, atTokenTook)
      } yield {
        // アカウント情報の取得
        getAccountInfo(state).map { case (accountInfo, state) =>
          Logger.info(accountInfo.toString)
          userService.upsertAccountInfo(accountInfo, Some(0), Some(state))
        }
        state
      }
      et
    })
  }

  /**
    * 残っていたセッションを通してログイン
    *
    * @param request
    * @tparam T
    * @return
    */
  def loginFromSession[T]()
                         (implicit request: MessagesRequest[T])
    : EitherT[Future, AppError, SessionState] = {

    Logging("ToodledoService.loginFromSession", {
      val stateOpt = SessionUtil.getTdSessionState(request.session)
      stateOpt match {
        case Some(state) =>
          // セッションが有効ならば最初からアプリに飛ばす
          // アカウント情報の取得
          val et = getAccountInfo(state)
          val f = et.value.map {
            case Right((accountInfo, state)) =>
              Logger.info(accountInfo.toString)
              userService.upsertAccountInfo(accountInfo, Some(0), Some(state))
              Right(state)
            case Left(e) =>
              Logger.error(e.toString)
              Left(AppError.Error(e.toString))
          }
          EitherT(f)
        case None =>
          EitherT.leftT(AppError.Error("FIXME: message"))
      }
    })
  }

  /**
    * アカウント情報を取得して返却
    *
    * @param oldState
    * @param request
    * @tparam T
    * @return
    */
  def getAccountInfo[T](oldState: SessionState)
                       (implicit request: MessagesRequest[T])
    : EitherT[Future, AppError, (TdAccountInfo, SessionState)] = {

    Logging("ToodledoService.getAccountInfo", {
      EitherT.right(api.getAccountInfo(oldState))
    })
  }

  /**
    * タスクを同期で取得して返却(同期じゃないと再帰呼び出しが難しかった…)
    * 全件取得する処理も行う
    *
    * @param oldState
    * @param start
    * @param num
    * @param count
    * @param retry
    * @param request
    * @tparam T
    * @return
    */
  def getTasks[T](oldState: SessionState,
                  start: Int, num: Int, count: Int)
                 (implicit request: MessagesRequest[T])
    : EitherT[Future, AppError, (Seq[TdTask], Int, Int, SessionState)] = {

    Logging("ToodledoService.getTasks", {
      val f = api.getTasks(start, num, oldState)
        .map { case r @ (tasks, newNum, newTotal, newState) =>
          if (count + newNum >= newTotal) {
            Right(r)
          } else {
            val f2 = getTasks(newState, start + num, num, count + newNum)
            // FIXME: 同期で待ちたくない
            val r2 = Await.result(f2.value, Duration.Inf)
            r2
          }
      }
      EitherT(f)
    })
  }

  /**
    * 削除されたタスクを取得する
    *
    * @param oldState
    * @param request
    * @tparam T
    * @return
    */
  def getDeletedTasks[T](oldState: SessionState)
                        (implicit request: MessagesRequest[T])
    : Either[AppError, (Seq[TdDeletedTask], SessionState)] = {

    Logging("ToodledoService.getDeletedTasks", {
      val f1 = for {
        accountInfoAndState <- api.getDeletedTasks(None, oldState)
      } yield {
        val (deletedTasks, state2) = accountInfoAndState
        Logger.info(deletedTasks.toString)
        Right((deletedTasks, state2))
      }
      // 例外をAppErrorに変換
      val f2 = f1.recover {
        case ex: Throwable =>
          Left(AppError.Exception(ex))
      }
      // FIXME: 同期で待ちたくない
      Await.result(f2, Duration.Inf)
    })
  }
}
