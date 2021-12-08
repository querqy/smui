package models

import anorm.SqlParser.get
import anorm._
import play.api.libs.Files.logger
import play.api.libs.json._

import java.sql.Connection
import java.time.LocalDateTime
import scala.collection.mutable

class UserId(id: String) extends Id(id)
object UserId extends IdObject[UserId](new UserId(_))

/**
  * Defines a tag that can be assigned to a search input
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

  val ID = "id"
  val USERNAME = "username"
  val EMAIL = "email"
  val PASSWORD = "password"
  val ADMIN = "admin"
  val LAST_UPDATE = "last_update"

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

  def insert(newUser: User)(implicit connection: Connection): UserId = {
    SQL"insert into #$TABLE_NAME (id, username, email, password, admin, last_update) values (${newUser.id}, ${newUser.username}, ${newUser.email}, ${newUser.password}, ${newUser.admin}, ${LocalDateTime.now()})".execute()
    newUser.id
  }

  def loadAll()(implicit connection: Connection): Seq[User] = {
    SQL"select * from #$TABLE_NAME order by username asc, email asc"
      .as(sqlParser.*)
  }

  def getUserById(userId: String)(implicit connection: Connection): User = {
    SQL"select * from #$TABLE_NAME where id = $userId order by username asc, email asc"
      .as(sqlParser.*).head
  }

  def getUserByEmail(email: String)(implicit connection: Connection): User = {
    SQL"select * from #$TABLE_NAME where email = $email order by username asc, email asc"
      .as(sqlParser.*).head
  }

  def getUserByUsername(username: String)(implicit connection: Connection): User = {
    SQL"select * from #$TABLE_NAME where username = $username order by username asc, email asc"
      .as(sqlParser.*).head
  }

  def deleteByIds(ids: Seq[UserId])(implicit connection: Connection): Unit = {
    for (idGroup <- ids.grouped(100)) {
      SQL"delete from #$TABLE_NAME where #$ID in ($idGroup)".executeUpdate()
    }
  }


}

object UserDAO {

  // Map username -> User
  //private val users: mutable.Map[String, User] = mutable.Map()
  private val users = mutable.Map(
    "user@example.com" -> User(username = "Example User", email = "user@example.com", password = "password")
  )

  def getUser(email: String): Option[User] = {
    users.get(email)
  }

  // this method should be thread safe
  def addUser(name: String, email: String, password: String): Option[User] = {

    // check if user already exists and return error if it does
    if(users.contains(email)) {
      Option.empty
    } else {
      val user = User(username = name, email = email, password = password)
      users.put(email, user)
      Option(user)
    }
  }

}
