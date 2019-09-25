package utils

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatest.Suite
import play.api.db.evolutions.Evolutions
import play.api.db.Databases

trait PostgresTestBase extends ForAllTestContainer { self: Suite =>

  override val container: PostgreSQLContainer = PostgreSQLContainer()

  protected lazy val db = Databases(container.driverClassName, container.jdbcUrl,
    config = Map("username" -> container.username, "password" -> container.password))

  override def afterStart(): Unit = {
    Evolutions.applyEvolutions(db)
  }

}
