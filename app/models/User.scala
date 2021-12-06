package models

import scala.collection.mutable

case class User(email: String, password: String)

object UserDAO {

  // Map username -> User
  //private val users: mutable.Map[String, User] = mutable.Map()
  private val users = mutable.Map(
    "user@example.com" -> User("user@example.com", "password")
  )

  def getUser(email: String): Option[User] = {
    users.get(email)
  }

  // this method should be thread safe
  def addUser(email: String, password: String): Option[User] = {

    // check if user already exists and return error if it does
    if(users.contains(email)) {
      Option.empty
    } else {
      val user = User(email, password)
      users.put(email, user)
      Option(user)
    }
  }

}
