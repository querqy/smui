package models

import org.h2.jdbc.JdbcBatchUpdateException
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import utils.WithInMemoryDB

class UserSpec extends FlatSpec with Matchers with BeforeAndAfterEach with WithInMemoryDB {

  private val users = Seq(
    User.create("name1", "email1", "good rule"),
    User.create("name2", "email2", "MO", admin = true),
    User.create("name3", "email3", "MO_DE", admin = true),
    User.create("name4", "email4", "MO_AT", admin = true)
  )

  // Accuracy of lastUpdate before/after database insert should omit nano seconds
  def adjustedTimeAccuracy(users: Seq[User]): Seq[User] = users.map(user =>
    user.copy(lastUpdate = user.lastUpdate.withNano(0))
  )

  "User" should "be saved to the database and read out again" in {
    db.withConnection { implicit connection =>
      User.insert(users: _*)
      val loaded = User.loadAll()
      adjustedTimeAccuracy(loaded).toSet shouldBe adjustedTimeAccuracy(users).toSet
    }

  }

  it should "not allow inserting the same username more than once" in {
    val user1 = User.create("name10", "email10", "good rule")
    val user2 = User.create("name10", "email11", "good rule1")
    db.withConnection { implicit connection =>
      intercept[JdbcBatchUpdateException] {
        User.insert(user1, user2)
      }
    }
  }

  it should "not allow inserting the same email more than once" in {
    val user1 = User.create("name10", "email10", "good rule")
    val user2 = User.create("name11", "email10", "good rule1")
    db.withConnection { implicit connection =>
      intercept[JdbcBatchUpdateException] {
        User.insert(user1, user2)
      }
    }
  }

}
