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
  import slick.collection.heterogeneous._
  import slick.collection.heterogeneous.syntax._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = PlayEvolutions.schema ++ Tasks.schema ++ Users.schema
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

  /** Row type of table Tasks */
  type TasksRow = HCons[Long,HCons[String,HCons[String,HCons[Int,HCons[Int,HCons[Int,HCons[Int,HCons[Int,HCons[Int,HCons[Int,HCons[String,HCons[Int,HCons[Int,HCons[Int,HCons[String,HCons[Int,HCons[Int,HCons[Int,HCons[Int,HCons[String,HCons[Int,HCons[Int,HCons[Int,HCons[Option[String],HNil]]]]]]]]]]]]]]]]]]]]]]]]
  /** Constructor for TasksRow providing default values if available in the database schema. */
  def TasksRow(id: Long, taskId: String, title: String, lastSync: Int, modified: Int, completed: Int, folderId: Int, contextId: Int, goalId: Int, locationId: Int, tag: String, startDate: Int, dueDate: Int, remind: Int, repeat: String, status: Int, star: Int, priority: Int, added: Int, note: String, parentId: Int, childrenCount: Int, orderNo: Int, meta: Option[String] = None): TasksRow = {
    id :: taskId :: title :: lastSync :: modified :: completed :: folderId :: contextId :: goalId :: locationId :: tag :: startDate :: dueDate :: remind :: repeat :: status :: star :: priority :: added :: note :: parentId :: childrenCount :: orderNo :: meta :: HNil
  }
  /** GetResult implicit for fetching TasksRow objects using plain SQL queries */
  implicit def GetResultTasksRow(implicit e0: GR[Long], e1: GR[String], e2: GR[Int], e3: GR[Option[String]]): GR[TasksRow] = GR{
    prs => import prs._
      <<[Long] :: <<[String] :: <<[String] :: <<[Int] :: <<[Int] :: <<[Int] :: <<[Int] :: <<[Int] :: <<[Int] :: <<[Int] :: <<[String] :: <<[Int] :: <<[Int] :: <<[Int] :: <<[String] :: <<[Int] :: <<[Int] :: <<[Int] :: <<[Int] :: <<[String] :: <<[Int] :: <<[Int] :: <<[Int] :: <<?[String] :: HNil
  }
  /** Table description of table tasks. Objects of this class serve as prototypes for rows in queries. */
  class Tasks(_tableTag: Tag) extends profile.api.Table[TasksRow](_tableTag, "tasks") {
    def * = id :: taskId :: title :: lastSync :: modified :: completed :: folderId :: contextId :: goalId :: locationId :: tag :: startDate :: dueDate :: remind :: repeat :: status :: star :: priority :: added :: note :: parentId :: childrenCount :: orderNo :: meta :: HNil

    /** Database column id SqlType(bigserial), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("id", O.AutoInc, O.PrimaryKey)
    /** Database column task_id SqlType(varchar), Length(255,true) */
    val taskId: Rep[String] = column[String]("task_id", O.Length(255,varying=true))
    /** Database column title SqlType(varchar), Length(255,true) */
    val title: Rep[String] = column[String]("title", O.Length(255,varying=true))
    /** Database column last_sync SqlType(int4) */
    val lastSync: Rep[Int] = column[Int]("last_sync")
    /** Database column modified SqlType(int4) */
    val modified: Rep[Int] = column[Int]("modified")
    /** Database column completed SqlType(int4) */
    val completed: Rep[Int] = column[Int]("completed")
    /** Database column folder_id SqlType(int4) */
    val folderId: Rep[Int] = column[Int]("folder_id")
    /** Database column context_id SqlType(int4) */
    val contextId: Rep[Int] = column[Int]("context_id")
    /** Database column goal_id SqlType(int4) */
    val goalId: Rep[Int] = column[Int]("goal_id")
    /** Database column location_id SqlType(int4) */
    val locationId: Rep[Int] = column[Int]("location_id")
    /** Database column tag SqlType(varchar), Length(255,true) */
    val tag: Rep[String] = column[String]("tag", O.Length(255,varying=true))
    /** Database column start_date SqlType(int4) */
    val startDate: Rep[Int] = column[Int]("start_date")
    /** Database column due_date SqlType(int4) */
    val dueDate: Rep[Int] = column[Int]("due_date")
    /** Database column remind SqlType(int4) */
    val remind: Rep[Int] = column[Int]("remind")
    /** Database column repeat SqlType(varchar), Length(255,true) */
    val repeat: Rep[String] = column[String]("repeat", O.Length(255,varying=true))
    /** Database column status SqlType(int4) */
    val status: Rep[Int] = column[Int]("status")
    /** Database column star SqlType(int4) */
    val star: Rep[Int] = column[Int]("star")
    /** Database column priority SqlType(int4) */
    val priority: Rep[Int] = column[Int]("priority")
    /** Database column added SqlType(int4) */
    val added: Rep[Int] = column[Int]("added")
    /** Database column note SqlType(text) */
    val note: Rep[String] = column[String]("note")
    /** Database column parent_id SqlType(int4) */
    val parentId: Rep[Int] = column[Int]("parent_id")
    /** Database column children_count SqlType(int4) */
    val childrenCount: Rep[Int] = column[Int]("children_count")
    /** Database column order_no SqlType(int4) */
    val orderNo: Rep[Int] = column[Int]("order_no")
    /** Database column meta SqlType(text), Default(None) */
    val meta: Rep[Option[String]] = column[Option[String]]("meta", O.Default(None))

    /** Uniqueness Index over (taskId) (database name ix_tasks_task_id) */
    val index1 = index("ix_tasks_task_id", taskId :: HNil, unique=true)
  }
  /** Collection-like TableQuery object for table Tasks */
  lazy val Tasks = new TableQuery(tag => new Tasks(tag))

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

    /** Uniqueness Index over (userId) (database name ix_users_user_id) */
    val index1 = index("ix_users_user_id", userId, unique=true)
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))
}
