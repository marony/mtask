package com.binbo_kodakusan.mtask.services

import com.binbo_kodakusan.mtask.models.TdAccountInfo
import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}

class UserService @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit userQuery: UserQuery, ec: ExecutionContext)
  extends HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  def upsertAccountInfo(accountInfo: TdAccountInfo): Future[Unit] = {
    ???
  }
}
