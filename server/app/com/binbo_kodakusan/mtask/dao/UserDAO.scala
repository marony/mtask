package com.binbo_kodakusan.mtask.dao

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

import com.binbo_kodakusan.mtask.models.User
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

class UserDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Users = TableQuery[UsersTable]

  def all(): Future[Seq[User]] = db.run(Users.result)

  def insert(User: User): Future[Unit] = db.run(Users += User).map { _ => () }

  private class UsersTable(tag: Tag) extends Table[User](tag, "User") {

    def name = column[String]("NAME", O.PrimaryKey)
    def color = column[String]("COLOR")

    def * = (name, color) <> (User.tupled, User.unapply)
  }
}