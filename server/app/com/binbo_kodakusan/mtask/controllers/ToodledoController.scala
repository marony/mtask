package com.binbo_kodakusan.mtask.controllers

import cats.data._
import cats.implicits._
import com.binbo_kodakusan.mtask.Constants
import com.binbo_kodakusan.mtask.models.{TdAccountInfo, TdDeletedTask, TdSessionState, TdTask}
import javax.inject._
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.{JsValue, Json}
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
    val clientId = config.get[String]("toodledo.client_id")
    val secret = config.get[String]("toodledo.secret")
    val device = request.headers("User-Agent")
    // まだat_token_tookはない可能性がある
    val atTokenTook = request.session.get(Constants.SessionName.TD_AT_TOKEN_TOOK).map(_.toLong)
    val oldStateOpt = EitherT[Future, AppError, String] {
      request.session.get(Constants.SessionName.TD_STATE)
        match { case Some(v) => Future.successful(Right(v)) case None => Future.successful(Left(AppError.NoError())) }
    }

    // EitherT[Future, AppError, Result]の正常2tygロジック
    val et: EitherT[Future, AppError, Result] = for {
      oldState <- oldStateOpt
      r1 <- Toodledo.checkState(oldState, state, error)
      tdState <- Toodledo.getAccessToken(url, code, clientId, secret, device, atTokenTook)
    } yield {
      // アカウント情報の取得
      getAccountInfoInternal(request, Some(tdState)).map { accountInfo =>
        Logger.info(accountInfo.toString)
        // TODO: データベースに保存する
      }
      // うまくいったらReactのアプリを表示
      val session = SessionUtil.setTdSession(
        // stateをセッションから削除
        SessionUtil.remove(request.session, Constants.SessionName.TD_STATE),
        // セッションに色々設定
        tdState)
      Redirect(routes.HomeController.app)
        .withSession(session)
    }
    // Future[Result](EitherT.value)のエラー系ロジック
    EitherTUtil.eitherT2Error(et, (left: AppError) => {
      Logger.error(left.toString)
      Redirect(routes.HomeController.index)
        .flashing("danger" -> left.toString)
        .withNewSession
    }, (ex: Throwable) => {
      LogUtil.errorEx(ex)
      Redirect(routes.HomeController.index)
        .flashing("danger" -> ex.toString)
        .withNewSession
    })
  }

  /**
    * アカウント情報を取得する
    *
    * @return
    */
  def getAccountInfo() = Action { implicit request =>
    Logger.info(s"Toodledo::getAccountInfo called")

    // タスクを取得する
    val tdState = SessionUtil.getTdSessionState(request.session)
    val r: Either[AppError, (TdAccountInfo, TdSessionState)] = getAccountInfoInternal(request, tdState)

    r match {
      case Right(r) =>
        val accountInfo = r._1
        val tdState = r._2
        Logger.info(s"accountInfo = $accountInfo, tdState = $tdState")

        val session = SessionUtil.setTdSession(
          request.session,
          // セッションに色々設定
          tdState)

        Ok(Json.toJson(accountInfo.toShared))
          .withSession(session)
      case Left(e) =>
        Logger.error(e.toString)
        Redirect(routes.HomeController.index)
          .flashing("danger" -> e.toString)
          .withNewSession
    }
  }

  /**
    * タスク一覧を取得する
    * 削除済みのタスクは取り除く
    *
    * @return
    */
  def getTasks() = Action { implicit request =>
    Logger.info(s"Toodledo::getTasks called")

    // タスクを取得する
    val num = 1000
    val tdState = SessionUtil.getTdSessionState(request.session)

    val r1 = getTasksInternal(request, tdState, 0, num, 0)
    r1 match {
      case Right((tasks, num, total, tdState1)) =>
        Logger.info(s"num = $num, total = $total, tasks = $tasks, tdState = $tdState1")
        val r2 = getDeletedTasksInternal(request, Some(tdState1))
        r2 match {
          case Right((deletedTasks, tdState2)) =>
            Logger.info(s"deletedTasks = $deletedTasks, tdState = $tdState2")
            val session = SessionUtil.setTdSession(
              request.session,
              // セッションに色々設定
              tdState2)

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
  }

  /**
    * アクセストークン再取得
    *
    * @param request
    * @tparam T
    * @return
    */
  private[this] def refreshAccessTokenInternal[T](request: Request[T], oldTdStateOpt: Option[TdSessionState]): Option[TdSessionState] = {
    val url = config.get[String]("toodledo.token.url")
    val clientId = config.get[String]("toodledo.client_id")
    val secret = config.get[String]("toodledo.secret")
    val device = request.headers("User-Agent")

    if (oldTdStateOpt.isEmpty) {
      None
    } else {
      val et: EitherT[Future, AppError, Option[TdSessionState]] = for {
        r1 <- Toodledo.refreshAccessToken(url, oldTdStateOpt.get.refreshToken,
          clientId, secret, device, oldTdStateOpt.get.atTokenTook)
      } yield {
        Some(r1)
      }
      val f: Future[Either[AppError, Option[TdSessionState]]] = et.value.recover {
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
  private[this] def getAccountInfoInternal[T](request: Request[T], oldTdStateOpt: Option[TdSessionState], retry: Boolean = false)
    : Either[AppError, (TdAccountInfo, TdSessionState)] = {
    val url = config.get[String]("toodledo.account_info.url")

    if (oldTdStateOpt.isEmpty) {
      Left(AppError.Error("TODO: message"))
    } else {
      val et: EitherT[Future, AppError, (TdAccountInfo, TdSessionState)] = for {
        accountInfoAndState <- Toodledo.getAccountInfo(url, oldTdStateOpt.get)
      } yield {
        val (Some(accountInfo), tdState2) = accountInfoAndState
        Logger.info(accountInfo.toString)
        (accountInfo, tdState2)
      }
      // 例外をAppErrorに変換
      val f: Future[Either[AppError, (TdAccountInfo, TdSessionState)]] = et.value.recover {
        case ex: Throwable =>
          Left(AppError.Exception(ex))
      }
      Await.ready(f, Duration.Inf)
      val r: Either[AppError, (TdAccountInfo, TdSessionState)] = f.value.get.get
      r match {
        case Right(r2: (TdAccountInfo, TdSessionState)) =>
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
                val tdState = refreshAccessTokenInternal(request, Some(tdOldState))
                getAccountInfoInternal(request, tdState, true)
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
  private[this] def getTasksInternal[T](request: Request[T], oldTdStateOpt: Option[TdSessionState],
                                        start: Int, num: Int, count: Int, retry: Boolean = false)
    : Either[AppError, (Seq[TdTask], Int, Int, TdSessionState)] = {

    val url = config.get[String]("toodledo.get_task.url")

    if (oldTdStateOpt.isEmpty) {
      Left(AppError.Error("TODO: message"))
    } else {
      val et: EitherT[Future, AppError, (Seq[TdTask], Int, Int, TdSessionState)] = for {
        tasksAndState <- Toodledo.getTasks(url, start, num, oldTdStateOpt.get)
      } yield {
        val (tasks, num2, total, tdState2) = tasksAndState
        Logger.info(tasks.toString)
        (tasks, num2, total, tdState2)
      }
      // 例外をAppErrorに変換
      val f: Future[Either[AppError, (Seq[TdTask], Int, Int, TdSessionState)]] = et.value.recover {
        case ex: Throwable =>
          Left(AppError.Exception(ex))
      }
      Await.ready(f, Duration.Inf)
      val r: Either[AppError, (Seq[TdTask], Int, Int, TdSessionState)] = f.value.get.get
      r match {
        case Right(r2: (Seq[TdTask], Int, Int, TdSessionState)) =>
          // 成功したが、さらに取得できるならばする
          val tasks = r2._1
          val num2 = r2._2
          val total = r2._3
          val tdState = r2._4
          if (count + num2 >= total) {
            r
          } else {
            // 件数が足りなければ再取得する
            val r3 = getTasksInternal(request, Some(tdState), start + num, num, count + num2)
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
                val tdState = refreshAccessTokenInternal(request, Some(tdOldState))
                getTasksInternal(request, tdState, start, num, count, true)
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
  private[this] def getDeletedTasksInternal[T](request: Request[T], oldTdStateOpt: Option[TdSessionState], retry: Boolean = false)
  : Either[AppError, (Seq[TdDeletedTask], TdSessionState)] = {
    val url = config.get[String]("toodledo.deleted_task.url")

    if (oldTdStateOpt.isEmpty) {
      Left(AppError.Error("TODO: message"))
    } else {
      val et: EitherT[Future, AppError, (Seq[TdDeletedTask], TdSessionState)] = for {
        accountInfoAndState <- Toodledo.getDeletedTasks(url, None, oldTdStateOpt.get)
      } yield {
        val (deletedTasks, tdState2) = accountInfoAndState
        Logger.info(deletedTasks.toString)
        (deletedTasks, tdState2)
      }
      // 例外をAppErrorに変換
      val f: Future[Either[AppError, (Seq[TdDeletedTask], TdSessionState)]] = et.value.recover {
        case ex: Throwable =>
          Left(AppError.Exception(ex))
      }
      Await.ready(f, Duration.Inf)
      val r: Either[AppError, (Seq[TdDeletedTask], TdSessionState)] = f.value.get.get
      r match {
        case Right(r2: (Seq[TdDeletedTask], TdSessionState)) =>
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
                val tdState = refreshAccessTokenInternal(request, Some(tdOldState))
                getDeletedTasksInternal(request, tdState, true)
              case _ => Left(e)
            }
          }
      }
    }
  }
}

object ToodledoController {
}
