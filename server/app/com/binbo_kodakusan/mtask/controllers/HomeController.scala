package com.binbo_kodakusan.mtask.controllers

import com.binbo_kodakusan.mtask.Constants
import com.binbo_kodakusan.mtask.models.{SessionState, Tables}
import javax.inject._
import play.api.{Configuration, Logger}
import play.api.mvc._
import com.binbo_kodakusan.mtask.services.{ToodledoApi, ToodledoService, UserQuery, UserService}
import com.binbo_kodakusan.mtask.util.SessionUtil

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

@Singleton
class HomeController @Inject()
  (config: Configuration, cc: ControllerComponents)
  (userDAO: UserQuery)
  (implicit ec: ExecutionContext,
   usersService: UserService,
   tdApi: ToodledoApi, tdService: ToodledoService,
   webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

  /**
    * トップページ
    *
    * @return
    */
  def index = Action { implicit request =>
    SessionUtil.getTdSessionState(request.session) match {
      case Some(state) =>
        // セッションが有効ならば最初からアプリに飛ばす
        // アカウント情報の取得
        tdService.getAccountInfo(request, Some(state)) match {
          case Right((accountInfo, _: SessionState)) =>
            Logger.info(accountInfo.toString)
            usersService.upsertAccountInfo(accountInfo, Some(0), Some(state))
            // うまくいったらReactのアプリを表示
            val session = SessionUtil.setTdSession(
              // stateをセッションから削除
              SessionUtil.remove(request.session, Constants.SessionName.TD_STATE),
              // セッションに色々設定
              state)
            Redirect(routes.HomeController.app)
              .withSession(session)
          case Left(e) =>
            Logger.error(e.toString)
            Ok(views.html.index("タイトルだよ"))
        }
      case None =>
        Ok(views.html.index("タイトルだよ"))
    }
  }

  /**
    * Reactアプリ
    * @return
    */
  def app = Action { implicit request =>
    Logger.info("START: Application(index)")

    Ok(views.html.app("タイトルだよ", "Message"))
  }
}
