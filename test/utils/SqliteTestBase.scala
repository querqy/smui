package utils

import java.io.File

import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}

trait SqliteTestBase extends BeforeAndAfterAll { self: Suite =>

  private lazy val dbFile = File.createTempFile("sqlitetest", ".db")

  lazy val db: Database = {
    // Use a temp file for the database - in-memory DB cannot be used
    // since it would be a different DB for each connection in the connection pool
    // (see https://www.sqlite.org/inmemorydb.html)
    val d = Databases("org.sqlite.JDBC", s"jdbc:sqlite:${dbFile.getAbsolutePath}")
    Evolutions.applyEvolutions(d)
    d
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    db.shutdown()
    dbFile.delete()
  }

}
