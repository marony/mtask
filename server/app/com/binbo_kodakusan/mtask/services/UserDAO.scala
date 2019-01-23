package com.binbo_kodakusan.mtask.services

import com.binbo_kodakusan.mtask.models.Tables
import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}

class UserDAO @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  def all(): Future[Seq[Tables.Users#TableElementType]] = db.run(Tables.Users.result)

  def getById(id: Long): Future[Option[Tables.Users#TableElementType]] =
    db.run(Tables.Users.filter(_.id === id).result).map(_.headOption)
  def getByUserId(userId: String): Future[Option[Tables.Users#TableElementType]] =
    db.run(Tables.Users.filter(_.userId === userId).result).map(_.headOption)

  def insert(user: Tables.UsersRow): Future[Unit] =
    db.run(Tables.Users += user).map(_ => ())
  def updateAlias(userId: String, alias: String): Future[Unit] =
    db.run(Tables.Users.filter(_.userId === userId).map(_.alias).update(alias)).map(_ => ())
}
