package com.binbo_kodakusan.mtask.controllers

import com.binbo_kodakusan.mtask.dao.UserDAO
import com.binbo_kodakusan.mtask.shared.SharedMessages
import com.binbo_kodakusan.mtask.models.Tables
import javax.inject._
import play.Logger
import play.api.Configuration
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()
  (config: Configuration, cc: ControllerComponents)
  (userDAO: UserDAO)
  (implicit ec: ExecutionContext, webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

  def index = Action {
    Logger.info("START: Application(index)")

    var value = config.get[String]("application.mode").toString

    {
      userDAO.all().map { (users: Seq[Tables.UsersRow]) =>
        users.map { (user: Tables.UsersRow) =>
          value = value + "*" + "A:" + user.username
        }
      }
      userDAO.all().map { (users: Seq[Tables.UsersRow]) =>
        users.map { (user: Tables.UsersRow) =>
          value = value + "*" + "B:" + user.username
        }
      }.value
    }

    Logger.info("END: Application(index)")

    Ok(views.html.index(SharedMessages.itWorks + ":" + value))
  }

}
