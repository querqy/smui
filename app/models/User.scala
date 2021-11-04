package models

import scala.collection.mutable

case class User(username: String, password: String)

object UserDAO {

  // Map username -> User
  //private val users: mutable.Map[String, User] = mutable.Map()
  private val users = mutable.Map(
    "user001" -> User("user001", "pass001")
  )

  def getUser(username: String): Option[User] = {
    users.get(username)
  }

  // this method should be thread safe
  def addUser(username: String, password: String): Option[User] = {

    // check if user already exists and return error if it does
    if(users.contains(username)) {
      Option.empty
    } else {
      val user = User(username, password)
      users.put(username, user)
      Option(user)
    }
  }

}
