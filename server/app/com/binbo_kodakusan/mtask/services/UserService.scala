package com.binbo_kodakusan.mtask.services

import com.binbo_kodakusan.mtask.models.{SessionState, Tables, TdAccountInfo}
import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.PostgresProfile

import scala.concurrent.{ExecutionContext, Future}

class UserService @Inject()
  (protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit userQuery: UserQuery, ec: ExecutionContext)
  extends HasDatabaseConfigProvider[PostgresProfile] {

  import profile.api._

  def upsertAccountInfo(accountInfo: TdAccountInfo, lastSync: Option[Int], state: Option[SessionState]): Future[Unit] = {
    val user = Tables.UsersRow(accountInfo.userid, accountInfo.alias, accountInfo.email,
      state.map(_.token), state.map(_.refreshToken), if (lastSync.isDefined) lastSync.get else 0,
      accountInfo.lastedit_folder, accountInfo.lastedit_context,
      accountInfo.lastedit_goal, accountInfo.lastedit_location, accountInfo.lastedit_task,
      accountInfo.lastdelete_task, accountInfo.lastedit_note, accountInfo.lastdelete_note,
      accountInfo.lastedit_list, accountInfo.lastedit_outline)
    db.run(userQuery.all.insertOrUpdate(user).transactionally).map(_ => ())
  }
}
