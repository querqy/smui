package models

import org.scalatest.{BeforeAndAfterEach, Suite}
import play.api.db.{Database, Databases}
import play.api.db.evolutions.Evolutions

trait WithInMemoryDB extends BeforeAndAfterEach { self: Suite =>

  protected var db: Database = _

  override protected def beforeEach(): Unit = {
    db = Databases.inMemory(config = Map("MODE" -> "MYSQL"))
    Evolutions.applyEvolutions(db)
  }

  override protected def afterEach(): Unit = {
    db.shutdown()
  }

}
