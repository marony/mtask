package com.binbo_kodakusan.mtask.controllers

import cats.data._
import cats.implicits._
import com.binbo_kodakusan.mtask.Constants
import com.binbo_kodakusan.mtask.models.td
import com.binbo_kodakusan.mtask.models.td.{SessionState, Task}
import javax.inject._
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.JsValue
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
    * タスクを同期で取得して返却(同期じゃないと再帰呼び出しが難しかった…)
    *
    * 全件取得する処理とアクセストークン再取得も行う
    *
    * @param request
    * @param start
    * @param num
    * @param count
    * @tparam T
    * @return
    */
  private[this] def getTasksInternal[T](request: Request[T],
                                        start: Int, num: Int, count: Int)
    : Either[AppError, (Seq[td.Task], Int, Int, td.SessionState)] = {

    val url = config.get[String]("toodledo.get_task.url")
    val oldStateOpt = EitherT[Future, AppError, td.SessionState] {
      SessionUtil.getTdSessionState(request.session)
      match { case Some(v) => Future.successful(Right(v)) case None => Future.successful(Left(AppError.NoError())) }
    }
    val et: EitherT[Future, AppError, (Seq[td.Task], Int, Int, td.SessionState)] = for {
      tdState <- oldStateOpt
      tasksAndState <- Toodledo.getTasks(url, start, num, tdState)
    } yield {
      val (Some(tasks), num2, total, tdState2) = tasksAndState
      Logger.info(tasks.toString)
      (tasks, num2, total, tdState2)
    }
    // 例外をAppErrorに変換
    val f: Future[Either[AppError, (Seq[Task], Int, Int, td.SessionState)]] = et.value.recover {
      case ex: Throwable =>
        Left(AppError.Exception(ex))
    }
    Await.ready(f, Duration.Inf)
    val r: Either[AppError, (Seq[Task], Int, Int, td.SessionState)] = f.value.get.get
    r match {
      case Right(r2: (Seq[td.Task], Int, Int, td.SessionState)) =>
        val tasks = r2._1
        val num2 = r2._2
        val total = r2._3
        val tdState = r2._4
        if (count + num2 >= total) {
          r
        } else {
          // 件数が足りなければ再取得する
          val r3 = getTasksInternal(request, start + num, num, count + num2)
          Right((tasks ++ r3.right.get._1, count + num2, total, r3.right.get._4))
        }
      case Left(e: AppError) =>
        e match {
          case AppError.TokenExpired(json: JsValue) =>
            // TODO: アクセストークンを再取得する
            Left(e)
          case _ => Left(e)
        }
    }
  }

  /**
    * タスク一覧を取得する
    * TODO: JSONを返却する
    *
    * @return
    */
  def getTasks() = Action { implicit request =>
    Logger.info(s"Toodledo::getTasks called")

    val num = 1000
    val r: Either[AppError, (Seq[td.Task], Int, Int, td.SessionState)] = getTasksInternal(request, 0, num, 0)

    r match {
      case Right(r) =>
        val tasks = r._1
        val num = r._2
        val total = r._3
        val tdState = r._4
        Logger.info(s"num = $num, total = $total, tasks = $tasks, tdState = $tdState")

        val session = SessionUtil.setTdSession(
          request.session,
          // セッションに色々設定
          tdState)

        Redirect(routes.HomeController.app)
          .withSession(session)
      case Left(e) =>
        Logger.error(e.toString)
        Redirect(routes.HomeController.index)
          .flashing("danger" -> e.toString)
          .withNewSession
    }
  }
}

object ToodledoController {
}
