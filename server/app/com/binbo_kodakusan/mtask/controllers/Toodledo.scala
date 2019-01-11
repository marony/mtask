package com.binbo_kodakusan.mtask.controllers

import javax.inject._

import play.api.{Configuration, Logger}
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class Toodledo @Inject()
  (config: Configuration, cc: ControllerComponents)
  (implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  /**
    * Toodledoの認証
    *
    * @return
    */
  def authorize() = Action.async { implicit request =>
    Logger.info(s"Toodledo::authorize called")
    ???
  }

  /**
    * Toodledoからの認証コールバック
    * @return
    */
  def callback() = Action.async { implicit request =>
    Logger.info(s"Toodledo::callback called")
    ???
  }
}
