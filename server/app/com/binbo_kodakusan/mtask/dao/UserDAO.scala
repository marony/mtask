package com.binbo_kodakusan.mtask.dao

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

import com.binbo_kodakusan.mtask.models.Tables
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.PostgresProfile

class UserDAO @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext)
    extends HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  def all() = db.run(Tables.Users.map(u => u).result)
}