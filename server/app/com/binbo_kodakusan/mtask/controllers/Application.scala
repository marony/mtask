package com.binbo_kodakusan.mtask.controllers

import javax.inject._

import com.binbo_kodakusan.mtask.shared.SharedMessages
import play.api.mvc._

@Singleton
class Application @Inject()
  (cc: ControllerComponents)
  (implicit webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

  def index = Action {
    Ok(views.html.index(SharedMessages.itWorks))
  }

}
