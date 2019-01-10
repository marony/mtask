package com.binbo_kodakusan.mtask.dao

import javax.inject.Inject

import com.binbo_kodakusan.mtask.models.Tables
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.PostgresProfile

class UserDAO @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
    extends HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  def all() = db.run(Tables.Users.result)
}
