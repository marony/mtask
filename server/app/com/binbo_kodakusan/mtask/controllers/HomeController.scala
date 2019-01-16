package com.binbo_kodakusan.mtask.controllers

import com.binbo_kodakusan.mtask.dao.UserDAO
import com.binbo_kodakusan.mtask.models.Tables
import javax.inject._
import play.api.{Configuration, Logger}
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

@Singleton
class HomeController @Inject()
  (config: Configuration, cc: ControllerComponents)
  (userDAO: UserDAO)
  (implicit ec: ExecutionContext, webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

  /**
    * トップページ
    *
    * @return
    */
  def index = Action { implicit request =>
    Ok(views.html.index("タイトルだよ"))
  }

  /**
    * Reactアプリ
    * @return
    */
  def app = Action { implicit request =>
    Logger.info("START: Application(index)")

    {
      val f = userDAO.all().map { (users: Seq[Tables.UsersRow]) =>
        users.map { (user: Tables.UsersRow) =>
          user.username
        }
      }
      Await.ready(f, Duration.Inf)
      f.value.get match {
        case Success(name) => name
        case Failure(ex) => Logger.error(ex.toString)
      }
    }

    Ok(views.html.app("タイトルだよ", "Message"))
  }
}
