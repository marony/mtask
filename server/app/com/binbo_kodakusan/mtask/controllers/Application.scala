package com.binbo_kodakusan.mtask.controllers

import com.binbo_kodakusan.mtask.dao.UserDAO
import com.binbo_kodakusan.mtask.shared.SharedMessages
import com.binbo_kodakusan.mtask.models.Tables
import com.binbo_kodakusan.mtask.models.Tables.UsersRow
import javax.inject._
import play.api.{Configuration, Play}
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()
  (config: Configuration, cc: ControllerComponents)
  (userDAO: UserDAO)
  (implicit ec: ExecutionContext, webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

  def index = Action {
    var value = config.get[String]("application.mode").toString

    {
//      val all = Tables.Users.map { user =>
//        value = value + "*" + user.username
//      }
      userDAO.all().map { (users: Seq[Tables.UsersRow]) =>
        users.map { (user: Tables.UsersRow) =>
          value = value + "*" + user.username
        }
      }.value
    }

    Ok(views.html.index(SharedMessages.itWorks + ":" + value))
  }

}
