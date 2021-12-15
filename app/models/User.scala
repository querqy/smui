package models

import anorm.Column.columnToString
import anorm.SqlParser.get
import anorm._
import play.api.libs.json._

import java.sql.Connection
import java.time.LocalDateTime

class UserId(id: String) extends Id(id)
object UserId extends IdObject[UserId](new UserId(_))

/**
  * Defines a user
  */
case class User(id: UserId = UserId(),
                    username: String,
                    email: String,
                    password: String,
                    admin: Boolean = false,
                    lastUpdate: LocalDateTime = LocalDateTime.now()) {

  import User._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    USERNAME -> username,
    EMAIL -> email,
    PASSWORD -> password,
    ADMIN -> (if (admin) 1 else 0),
    LAST_UPDATE -> lastUpdate
  )

  def displayValue: String = username + " (" + email + ")"

}

object User {
  val TABLE_NAME = "user"
  val TABLE_NAME_USER_2_TEAM = "user_2_team"

  val ID = "id"
  val USERNAME = "username"
  val EMAIL = "email"
  val PASSWORD = "password"
  val ADMIN = "admin"
  val LAST_UPDATE = "last_update"

  val USER_ID = "user_id"
  val TEAM_ID = "team_id"

  implicit val jsonReads: Reads[User] = Json.reads[User]

  private val defaultWrites: OWrites[User] = Json.writes[User]
  implicit val jsonWrites: OWrites[User] = OWrites[User] { user =>
    Json.obj("displayValue" -> user.displayValue) ++ defaultWrites.writes(user)
  }

  def create(username: String,
             email: String,
             password: String,
             admin: Boolean = false): User = {
    User(UserId(), username, email, password, admin, LocalDateTime.now())
  }

  val sqlParser: RowParser[User] = get[UserId](s"$TABLE_NAME.$ID") ~
    get[String](s"$TABLE_NAME.$USERNAME") ~
    get[String](s"$TABLE_NAME.$EMAIL") ~
    get[String](s"$TABLE_NAME.$PASSWORD") ~
    get[Int](s"$TABLE_NAME.$ADMIN") ~
    get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ username ~ email ~ password ~ admin ~ lastUpdate =>
    User(id, username, email, password, admin > 0, lastUpdate)
  }

  def insert(newUsers: User*)(implicit connection: Connection): Option[Int] = {
    var result: Array[Int]  = Array[Int]();
    if (newUsers.nonEmpty) {
      result = BatchSql(s"insert into $TABLE_NAME ($ID, $USERNAME, $EMAIL, $PASSWORD, $ADMIN, $LAST_UPDATE) " +
        s"values ({$ID}, {$USERNAME}, {$EMAIL}, {$PASSWORD}, {$ADMIN}, {$LAST_UPDATE})",
        newUsers.head.toNamedParameters,
        newUsers.tail.map(_.toNamedParameters): _*
      ).execute()
    }
    result.headOption
  }

  def getUser(userId: String)(implicit connection: Connection): Option[User] = {
    SQL"select * from #$TABLE_NAME where id = $userId order by #$USERNAME asc, #$EMAIL asc"
      .as(sqlParser.*).headOption
  }

  def update(id: UserId, username: String, email: String, password: String, admin: Boolean)(implicit connection: Connection): Int = {
    val adminInt = if (admin) 1 else 0
    SQL"update #$TABLE_NAME set #$USERNAME = $username, #$EMAIL = $email, #$PASSWORD = $password, #$ADMIN = $adminInt, #$LAST_UPDATE = ${LocalDateTime.now()} where #$ID = $id".executeUpdate()
  }

  def deleteByIds(ids: Seq[UserId])(implicit connection: Connection): Int = {
    var count = 0
    for (idGroup <- ids.grouped(100)) {
      count += SQL"delete from #$TABLE_NAME where #$ID in ($idGroup)".executeUpdate()
    }
    count
  }

  def loadAll()(implicit connection: Connection): Seq[User] = {
    SQL"select * from #$TABLE_NAME order by #$USERNAME asc, #$EMAIL asc"
      .as(sqlParser.*)
  }

  def getUserByEmail(email: String)(implicit connection: Connection): Option[User] = {
    SQL"select * from #$TABLE_NAME where #$EMAIL = $email order by #$USERNAME asc, #$EMAIL asc"
      .as(sqlParser.*).headOption
  }

  def getUserByUsername(username: String)(implicit connection: Connection): Option[User] = {
    SQL"select * from #$TABLE_NAME where #$USERNAME = $username order by #$USERNAME asc, #$EMAIL asc"
      .as(sqlParser.*).headOption
  }

  def getUser2Team(selectId: String, isLeftToRight: Boolean)(implicit connection: Connection): List[String] = {
    val selectFieldName = if (isLeftToRight) USER_ID else TEAM_ID
    val returnFieldName = if (isLeftToRight) TEAM_ID else USER_ID
    SQL"select #$returnFieldName from #$TABLE_NAME_USER_2_TEAM where #$selectFieldName=$selectId order by #$returnFieldName asc"
      .asTry(SqlParser.str(1).*).getOrElse(List.empty[String])
  }

  def addUser2Team(userId: String, teamId: String)(implicit connection: Connection): Int = {
    SQL"insert into #$TABLE_NAME_USER_2_TEAM (#$USER_ID, #$TEAM_ID, #$LAST_UPDATE) values ($userId, $teamId, ${LocalDateTime.now()})".executeUpdate()
  }

  def deleteUser2Team(userId: String, teamId: String)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME_USER_2_TEAM where #$USER_ID = $userId and #$TEAM_ID = $teamId".executeUpdate()
  }

}

