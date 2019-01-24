package com.binbo_kodakusan.mtask.services

import com.binbo_kodakusan.mtask.models.Tables
import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}

class UserQuery @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  def all() = Tables.Users

  def get(id: String) = Tables.Users.filter(_.id === id)

  def insert(user: Tables.UsersRow) = Tables.Users += user
  def updateAlias(id: String, alias: String) =
    Tables.Users.filter(_.id === id).map(_.alias).update(alias)
}
