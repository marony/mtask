package com.binbo_kodakusan.mtask.models

// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.PostgresProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = PlayEvolutions.schema ++ Users.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table PlayEvolutions
    *  @param id Database column id SqlType(int4), PrimaryKey
    *  @param hash Database column hash SqlType(varchar), Length(255,true)
    *  @param appliedAt Database column applied_at SqlType(timestamp)
    *  @param applyScript Database column apply_script SqlType(text), Default(None)
    *  @param revertScript Database column revert_script SqlType(text), Default(None)
    *  @param state Database column state SqlType(varchar), Length(255,true), Default(None)
    *  @param lastProblem Database column last_problem SqlType(text), Default(None) */
  case class PlayEvolutionsRow(id: Int, hash: String, appliedAt: java.sql.Timestamp, applyScript: Option[String] = None, revertScript: Option[String] = None, state: Option[String] = None, lastProblem: Option[String] = None)
  /** GetResult implicit for fetching PlayEvolutionsRow objects using plain SQL queries */
  implicit def GetResultPlayEvolutionsRow(implicit e0: GR[Int], e1: GR[String], e2: GR[java.sql.Timestamp], e3: GR[Option[String]]): GR[PlayEvolutionsRow] = GR{
    prs => import prs._
      PlayEvolutionsRow.tupled((<<[Int], <<[String], <<[java.sql.Timestamp], <<?[String], <<?[String], <<?[String], <<?[String]))
  }
  /** Table description of table play_evolutions. Objects of this class serve as prototypes for rows in queries. */
  class PlayEvolutions(_tableTag: Tag) extends profile.api.Table[PlayEvolutionsRow](_tableTag, "play_evolutions") {
    def * = (id, hash, appliedAt, applyScript, revertScript, state, lastProblem) <> (PlayEvolutionsRow.tupled, PlayEvolutionsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(hash), Rep.Some(appliedAt), applyScript, revertScript, state, lastProblem).shaped.<>({r=>import r._; _1.map(_=> PlayEvolutionsRow.tupled((_1.get, _2.get, _3.get, _4, _5, _6, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column hash SqlType(varchar), Length(255,true) */
    val hash: Rep[String] = column[String]("hash", O.Length(255,varying=true))
    /** Database column applied_at SqlType(timestamp) */
    val appliedAt: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("applied_at")
    /** Database column apply_script SqlType(text), Default(None) */
    val applyScript: Rep[Option[String]] = column[Option[String]]("apply_script", O.Default(None))
    /** Database column revert_script SqlType(text), Default(None) */
    val revertScript: Rep[Option[String]] = column[Option[String]]("revert_script", O.Default(None))
    /** Database column state SqlType(varchar), Length(255,true), Default(None) */
    val state: Rep[Option[String]] = column[Option[String]]("state", O.Length(255,varying=true), O.Default(None))
    /** Database column last_problem SqlType(text), Default(None) */
    val lastProblem: Rep[Option[String]] = column[Option[String]]("last_problem", O.Default(None))
  }
  /** Collection-like TableQuery object for table PlayEvolutions */
  lazy val PlayEvolutions = new TableQuery(tag => new PlayEvolutions(tag))

  /** Entity class storing rows of table Users
    *  @param id Database column id SqlType(bigserial), AutoInc, PrimaryKey
    *  @param userId Database column user_id SqlType(varchar), Length(255,true)
    *  @param alias Database column alias SqlType(varchar), Length(255,true)
    *  @param email Database column email SqlType(varchar), Length(255,true)
    *  @param accessToken Database column access_token SqlType(varchar), Length(255,true)
    *  @param refreshToken Database column refresh_token SqlType(varchar), Length(255,true)
    *  @param lastSync Database column last_sync SqlType(int4)
    *  @param lastEditFolder Database column last_edit_folder SqlType(int4)
    *  @param lastEditContext Database column last_edit_context SqlType(int4)
    *  @param lastEditGoal Database column last_edit_goal SqlType(int4)
    *  @param lastEditLocation Database column last_edit_location SqlType(int4)
    *  @param lastEditTask Database column last_edit_task SqlType(int4)
    *  @param lastDeleteTask Database column last_delete_task SqlType(int4)
    *  @param lastEditNote Database column last_edit_note SqlType(int4)
    *  @param lastDeleteNote Database column last_delete_note SqlType(int4)
    *  @param lastEditList Database column last_edit_list SqlType(int4)
    *  @param lastEditOutline Database column last_edit_outline SqlType(int4) */
  case class UsersRow(id: Long, userId: String, alias: String, email: String, accessToken: String, refreshToken: String, lastSync: Int, lastEditFolder: Int, lastEditContext: Int, lastEditGoal: Int, lastEditLocation: Int, lastEditTask: Int, lastDeleteTask: Int, lastEditNote: Int, lastDeleteNote: Int, lastEditList: Int, lastEditOutline: Int)
  /** GetResult implicit for fetching UsersRow objects using plain SQL queries */
  implicit def GetResultUsersRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Int]): GR[UsersRow] = GR{
    prs => import prs._
      UsersRow.tupled((<<[Long], <<[String], <<[String], <<[String], <<[String], <<[String], <<[Int], <<[Int], <<[Int], <<[Int], <<[Int], <<[Int], <<[Int], <<[Int], <<[Int], <<[Int], <<[Int]))
  }
  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends profile.api.Table[UsersRow](_tableTag, "users") {
    def * = (id, userId, alias, email, accessToken, refreshToken, lastSync, lastEditFolder, lastEditContext, lastEditGoal, lastEditLocation, lastEditTask, lastDeleteTask, lastEditNote, lastDeleteNote, lastEditList, lastEditOutline) <> (UsersRow.tupled, UsersRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(userId), Rep.Some(alias), Rep.Some(email), Rep.Some(accessToken), Rep.Some(refreshToken), Rep.Some(lastSync), Rep.Some(lastEditFolder), Rep.Some(lastEditContext), Rep.Some(lastEditGoal), Rep.Some(lastEditLocation), Rep.Some(lastEditTask), Rep.Some(lastDeleteTask), Rep.Some(lastEditNote), Rep.Some(lastDeleteNote), Rep.Some(lastEditList), Rep.Some(lastEditOutline)).shaped.<>({r=>import r._; _1.map(_=> UsersRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get, _12.get, _13.get, _14.get, _15.get, _16.get, _17.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(varchar), Length(255,true) */
    val userId: Rep[String] = column[String]("user_id", O.Length(255,varying=true))
    /** Database column alias SqlType(varchar), Length(255,true) */
    val alias: Rep[String] = column[String]("alias", O.Length(255,varying=true))
    /** Database column email SqlType(varchar), Length(255,true) */
    val email: Rep[String] = column[String]("email", O.Length(255,varying=true))
    /** Database column access_token SqlType(varchar), Length(255,true) */
    val accessToken: Rep[String] = column[String]("access_token", O.Length(255,varying=true))
    /** Database column refresh_token SqlType(varchar), Length(255,true) */
    val refreshToken: Rep[String] = column[String]("refresh_token", O.Length(255,varying=true))
    /** Database column last_sync SqlType(int4) */
    val lastSync: Rep[Int] = column[Int]("last_sync")
    /** Database column last_edit_folder SqlType(int4) */
    val lastEditFolder: Rep[Int] = column[Int]("last_edit_folder")
    /** Database column last_edit_context SqlType(int4) */
    val lastEditContext: Rep[Int] = column[Int]("last_edit_context")
    /** Database column last_edit_goal SqlType(int4) */
    val lastEditGoal: Rep[Int] = column[Int]("last_edit_goal")
    /** Database column last_edit_location SqlType(int4) */
    val lastEditLocation: Rep[Int] = column[Int]("last_edit_location")
    /** Database column last_edit_task SqlType(int4) */
    val lastEditTask: Rep[Int] = column[Int]("last_edit_task")
    /** Database column last_delete_task SqlType(int4) */
    val lastDeleteTask: Rep[Int] = column[Int]("last_delete_task")
    /** Database column last_edit_note SqlType(int4) */
    val lastEditNote: Rep[Int] = column[Int]("last_edit_note")
    /** Database column last_delete_note SqlType(int4) */
    val lastDeleteNote: Rep[Int] = column[Int]("last_delete_note")
    /** Database column last_edit_list SqlType(int4) */
    val lastEditList: Rep[Int] = column[Int]("last_edit_list")
    /** Database column last_edit_outline SqlType(int4) */
    val lastEditOutline: Rep[Int] = column[Int]("last_edit_outline")
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}
