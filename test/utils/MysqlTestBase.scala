package utils

import com.dimafeng.testcontainers.{ForAllTestContainer, MySQLContainer}
import org.scalatest.Suite
import play.api.db.evolutions.Evolutions
import play.api.db.Databases

trait MysqlTestBase extends ForAllTestContainer { self: Suite =>

  override val container: MySQLContainer = MySQLContainer()

  protected lazy val db = Databases(container.driverClassName, container.jdbcUrl,
    config = Map("username" -> container.username, "password" -> container.password))

  override def afterStart(): Unit = {
    Evolutions.applyEvolutions(db)
  }

}
