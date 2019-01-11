package com.binbo_kodakusan.mtask.controllers

import javax.inject._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._
import play.api.http.HttpEntity
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.binbo_kodakusan.mtask.shared.SharedMessages
import play.api.{Configuration, Logger}

@Singleton
class Toodledo @Inject()
  (config: Configuration, ws: WSClient, cc: ControllerComponents)
  (implicit ec: ExecutionContext, webJarsUtil: org.webjars.play.WebJarsUtil)
    extends AbstractController(cc) {

  /**
    * Toodledoの認証
    * Toodledoのログインページにリダイレクトする
    *
    * @return
    */
  def authorize() = Action {
    Logger.info(s"Toodledo::authorize called")

    val url = config.get[String]("toodledo.authorize.url")
    val client_id = config.get[String]("toodledo.client_id")
    val state = java.util.UUID.randomUUID.toString
    val scope = config.get[String]("toodledo.authorize.scope")

    Redirect(url, Map(
      "response_type" -> Seq("code"),
      "client_id" -> Seq(client_id),
      "state" -> Seq(state),
      "scope" -> Seq(scope)
    ))
      .withSession("td_state" -> state)
  }

  /**
    * Toodledoからの認証コールバック
    *
    * @return
    */
  def callback(code: String, state: String, error: Option[String]) = Action.async { implicit request =>
    Logger.info(s"Toodledo::callback called: $code, $state, $error")

    // TODO: errorのチェック
    // TODO: codeとstateのチェック
    ???
  }
}
