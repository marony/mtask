package com.binbo_kodakusan.mtask.services

import com.binbo_kodakusan.mtask.Constants
import javax.inject._
import com.binbo_kodakusan.mtask.models.{SessionState, TdAccountInfo, TdDeletedTask, TdTask}
import play.api.{Configuration, Logger}
import play.api.libs.ws.WSClient
import play.api.mvc.{MessagesRequest}
import com.binbo_kodakusan.mtask.util.{AppError, Logging}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

@Singleton
class ToodledoService @Inject()
  (api: ToodledoApi)
  (implicit config: Configuration, ws: WSClient, ec: ExecutionContext) {

  /**
    * Toodledoからのコールバックが正しいことをチェックする
    *
    * @param oldState
    * @param state
    * @param error
    * @param request
    * @tparam T
    * @return
    */
  def checkState[T](oldState: Option[String], state: String, error: Option[String])
                   (implicit request: MessagesRequest[T])
    : Either[AppError, Unit] = {

    Logging("ToodledoService.checkState", {
      val oldStateOpt = request.session.get(Constants.SessionName.TD_STATE)
      oldStateOpt match {
        case Some(oldState) =>
          val r1 = api.checkState(oldState, state, error)
          // FIXME: 同期で待ちたくない
          val r2 = Await.result(r1.value, Duration.Inf)
          r2
        case _ => Left(AppError.Error("FIXME: message"))
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
  def getAccessToken[T](code: String, clientId: String,
                        secret: String, device: String, atTokenTook: Option[Long])
    : Either[AppError, SessionState] = {

    Logging("ToodledoService.getAccessToken", {
      val f1 = for {
        state <- api.getAccessToken(code, clientId, secret, device, atTokenTook)
      } yield {
        Logger.info(state.toString)
        Right(state)
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

  /**
    * アカウント情報を同期で取得して返却
    *
    * @param oldState
    * @param request
    * @tparam T
    * @return
    */
  def getAccountInfo[T](oldState: SessionState)
                       (implicit request: MessagesRequest[T])
    : Either[AppError, (TdAccountInfo, SessionState)] = {

    Logging("ToodledoService.getAccountInfo", {
      val f1 = for {
        accountInfoAndState <- api.getAccountInfo(oldState)
      } yield {
        val (accountInfo, state2) = accountInfoAndState
        Logger.info(accountInfo.toString)
        Right((accountInfo, state2))
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
                  start: Int, num: Int, count: Int, retry: Boolean = false)
                 (implicit request: MessagesRequest[T])
    : Either[AppError, (Seq[TdTask], Int, Int, SessionState)] = {

    Logging("ToodledoService.getTasks", {
      val f1 = for {
        tasksAndState <- api.getTasks(start, num, oldState)
      } yield {
        val (tasks, num2, total, state2) = tasksAndState
        Logger.info(tasks.toString)
        Right((tasks, num2, total, state2))
      }
      // 例外をAppErrorに変換
      val f2 = f1.recover {
        case ex: Throwable =>
          Left(AppError.Exception(ex))
      }
      // FIXME: 同期で待ちたくない
      val r = Await.result(f2, Duration.Inf)
      r match {
        case Right(r2: (Seq[TdTask], Int, Int, SessionState)) =>
          // 成功したが、さらに取得できるならばする
          val tasks = r2._1
          val num2 = r2._2
          val total = r2._3
          val state = r2._4
          if (count + num2 >= total) {
            r
          } else {
            // 件数が足りなければ再取得する
            val r3 = getTasks(state, start + num, num, count + num2)
            Right((tasks ++ r3.right.get._1, count + num2, total, r3.right.get._4))
          }
        case Left(e) =>
          Left(e)
      }
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
