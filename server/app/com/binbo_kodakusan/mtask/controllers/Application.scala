package com.binbo_kodakusan.mtask.controllers

import javax.inject._
import com.binbo_kodakusan.mtask.dao.UserDAO
import com.binbo_kodakusan.mtask.shared.SharedMessages
import play.api.{Configuration, Play}
import play.api.mvc._

//import slick.driver.PostgresDriver.api._
//import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()
  (config: Configuration, userDao: UserDAO, cc: ControllerComponents)
  (implicit ec: ExecutionContext, webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

  def index = Action {
    var value = config.get[String]("application.mode").toString

    {
      val all = userDao.all().map { cats =>
        value = value + "*" + cats.toString
      }
    }

    Ok(views.html.index(SharedMessages.itWorks + ":" + value))
  }

}
