package com.binbo_kodakusan.mtask.services

import cats.data._
import cats.implicits._
import javax.inject._
import com.binbo_kodakusan.mtask.models.{SessionState, TdAccountInfo, TdDeletedTask, TdTask}
import play.api.{Configuration, Logger}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc.Request
import com.binbo_kodakusan.mtask.util.AppError

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

@Singleton
class ToodledoService @Inject()
  (api: ToodledoApi)
  (implicit config: Configuration, ws: WSClient, ec: ExecutionContext) {

  /**
    * アクセストークン再取得
    *
    * @param request
    * @tparam T
    * @return
    */
  def refreshAccessToken[T](request: Request[T], oldTdStateOpt: Option[SessionState])
    : Option[SessionState] = {

    val url = config.get[String]("toodledo.token.url")
    val clientId = config.get[String]("toodledo.client_id")
    val secret = config.get[String]("toodledo.secret")
    val device = request.headers("User-Agent")

    if (oldTdStateOpt.isEmpty) {
      None
    } else {
      val et: EitherT[Future, AppError, Option[SessionState]] = for {
        r1 <- api.refreshAccessToken(url, oldTdStateOpt.get.refreshToken,
          clientId, secret, device, oldTdStateOpt.get.atTokenTook)
      } yield {
        Some(r1)
      }
      val f: Future[Either[AppError, Option[SessionState]]] = et.value.recover {
        case ex: Throwable => Right(None)
      }
      Await.ready(f, Duration.Inf)
      f.value.get.get.right.get
    }
  }

  /**
    * アカウント情報を同期で取得して返却
    * アクセストークン再取得も行う
    *
    * @param request
    * @param oldTdState
    * @param retry
    * @tparam T
    * @return
    */
  def getAccountInfo[T](request: Request[T], oldTdStateOpt: Option[SessionState], retry: Boolean = false)
    : Either[AppError, (TdAccountInfo, SessionState)] = {

    val url = config.get[String]("toodledo.account_info.url")

    if (oldTdStateOpt.isEmpty) {
      Left(AppError.Error("TODO: message"))
    } else {
      val et: EitherT[Future, AppError, (TdAccountInfo, SessionState)] = for {
        accountInfoAndState <- api.getAccountInfo(url, oldTdStateOpt.get)
      } yield {
        val (Some(accountInfo), state2) = accountInfoAndState
        Logger.info(accountInfo.toString)
        (accountInfo, state2)
      }
      // 例外をAppErrorに変換
      val f: Future[Either[AppError, (TdAccountInfo, SessionState)]] = et.value.recover {
        case ex: Throwable =>
          Left(AppError.Exception(ex))
      }
      Await.ready(f, Duration.Inf)
      val r: Either[AppError, (TdAccountInfo, SessionState)] = f.value.get.get
      r match {
        case Right(r2: (TdAccountInfo, SessionState)) =>
          r
        case Left(e: AppError) =>
          // 失敗したがアクセストークン切れならば再取得する
          if (retry) {
            // 一度しかアクセストークンは取得しない
            Left(e)
          } else {
            e match {
              case AppError.TokenExpired(json: JsValue, tdOldState) =>
                // アクセストークンを再取得する
                val state = refreshAccessToken(request, Some(tdOldState))
                getAccountInfo(request, state, true)
              case _ => Left(e)
            }
          }
      }
    }
  }

  /**
    * タスクを同期で取得して返却(同期じゃないと再帰呼び出しが難しかった…)
    * 全件取得する処理とアクセストークン再取得も行う
    *
    * @param request
    * @param start
    * @param num
    * @param count
    * @tparam T
    * @return
    */
  def getTasks[T](request: Request[T], oldTdStateOpt: Option[SessionState],
                                        start: Int, num: Int, count: Int, retry: Boolean = false)
    : Either[AppError, (Seq[TdTask], Int, Int, SessionState)] = {

    val url = config.get[String]("toodledo.get_task.url")

    if (oldTdStateOpt.isEmpty) {
      Left(AppError.Error("TODO: message"))
    } else {
      val et: EitherT[Future, AppError, (Seq[TdTask], Int, Int, SessionState)] = for {
        tasksAndState <- api.getTasks(url, start, num, oldTdStateOpt.get)
      } yield {
        val (tasks, num2, total, state2) = tasksAndState
        Logger.info(tasks.toString)
        (tasks, num2, total, state2)
      }
      // 例外をAppErrorに変換
      val f: Future[Either[AppError, (Seq[TdTask], Int, Int, SessionState)]] = et.value.recover {
        case ex: Throwable =>
          Left(AppError.Exception(ex))
      }
      Await.ready(f, Duration.Inf)
      val r: Either[AppError, (Seq[TdTask], Int, Int, SessionState)] = f.value.get.get
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
            val r3 = getTasks(request, Some(state), start + num, num, count + num2)
            Right((tasks ++ r3.right.get._1, count + num2, total, r3.right.get._4))
          }
        case Left(e: AppError) =>
          // 失敗したがアクセストークン切れならば再取得する
          if (retry) {
            // 一度しかアクセストークンは取得しない
            Left(e)
          } else {
            e match {
              case AppError.TokenExpired(json: JsValue, tdOldState) =>
                // アクセストークンを再取得する
                val state = refreshAccessToken(request, Some(tdOldState))
                getTasks(request, state, start, num, count, true)
              case _ => Left(e)
            }
          }
      }
    }
  }

  /**
    * 削除されたタスクを取得する
    * アクセストークン再取得も行う
    *
    * @param request
    * @param oldTdStateOpt
    * @param retry
    * @tparam T
    * @return
    */
  def getDeletedTasks[T](request: Request[T], oldTdStateOpt: Option[SessionState], retry: Boolean = false)
    : Either[AppError, (Seq[TdDeletedTask], SessionState)] = {

    val url = config.get[String]("toodledo.deleted_task.url")

    if (oldTdStateOpt.isEmpty) {
      Left(AppError.Error("TODO: message"))
    } else {
      val et: EitherT[Future, AppError, (Seq[TdDeletedTask], SessionState)] = for {
        accountInfoAndState <- api.getDeletedTasks(url, None, oldTdStateOpt.get)
      } yield {
        val (deletedTasks, state2) = accountInfoAndState
        Logger.info(deletedTasks.toString)
        (deletedTasks, state2)
      }
      // 例外をAppErrorに変換
      val f: Future[Either[AppError, (Seq[TdDeletedTask], SessionState)]] = et.value.recover {
        case ex: Throwable =>
          Left(AppError.Exception(ex))
      }
      Await.ready(f, Duration.Inf)
      val r: Either[AppError, (Seq[TdDeletedTask], SessionState)] = f.value.get.get
      r match {
        case Right(r2: (Seq[TdDeletedTask], SessionState)) =>
          r
        case Left(e: AppError) =>
          // 失敗したがアクセストークン切れならば再取得する
          if (retry) {
            // 一度しかアクセストークンは取得しない
            Left(e)
          } else {
            e match {
              case AppError.TokenExpired(json: JsValue, tdOldState) =>
                // アクセストークンを再取得する
                val state = refreshAccessToken(request, Some(tdOldState))
                getDeletedTasks(request, state, true)
              case _ => Left(e)
            }
          }
      }
    }
  }
}
