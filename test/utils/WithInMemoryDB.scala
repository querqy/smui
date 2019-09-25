package utils

import org.scalatest.{BeforeAndAfterEach, Suite}
import play.api.db.evolutions.Evolutions
import play.api.db.{Database, Databases}

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
