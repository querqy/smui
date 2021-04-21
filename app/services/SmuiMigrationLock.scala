package services

import java.time.{LocalDateTime, Duration}
import java.sql.Connection

import anorm.SqlParser.get
import anorm._
import play.api.Logging

import scala.util.{Failure, Success, Try}

case class SmuiMigrationLock(migrationKey: String, lockTime: LocalDateTime, completed: Option[Boolean]) {

  import SmuiMigrationLock._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    MIGRATION_KEY -> migrationKey,
    LOCK_TIME -> lockTime,
    COMPLETED -> completed
  )

}

object SmuiMigrationLock extends Logging {

  val TABLE_NAME = "smui_migration_lock"

  val MIGRATION_KEY = "migration_key"
  val LOCK_TIME = "lock_time"
  val COMPLETED = "completed"

  val sqlParser: RowParser[SmuiMigrationLock] = {
    get[String](s"$MIGRATION_KEY") ~
      get[LocalDateTime](s"$LOCK_TIME") ~
      get[Option[Int]](s"$COMPLETED") map { case migrationKey ~ lockTime ~ completedRawOpt =>
      SmuiMigrationLock(
        migrationKey,
        lockTime,
        completedRawOpt match {
          case None => None
          case Some(completedRaw) => Some(completedRaw > 0)
        }
      )
    }
  }

  def create(migrationKey: String)(implicit connection: Connection): SmuiMigrationLock = {
    val migrationLock = SmuiMigrationLock(migrationKey, LocalDateTime.now(), None)
    SQL(s"insert into $TABLE_NAME ($MIGRATION_KEY, $LOCK_TIME) values ({$MIGRATION_KEY}, {$LOCK_TIME})")
      .on(migrationLock.toNamedParameters: _*).execute()
    migrationLock
  }

  def select(migrationKey: String)(implicit connection: Connection): Option[SmuiMigrationLock] = {
    SQL"select * from #$TABLE_NAME where #$MIGRATION_KEY = $migrationKey".as(sqlParser.*).headOption
  }

  def updateCompleted(migrationKey: String)(implicit connection: Connection) = {
    SQL"update #$TABLE_NAME set #$COMPLETED = 1 where #$MIGRATION_KEY = $migrationKey".executeUpdate()
  }

  private val GLOBAL_MIGRATION_TIMEOUT_SECONDS = 15*60

  def executeOnce(migrationKey: String, op: () => Unit)(implicit connection: Connection): Unit = {
    // migration logic will only be completed once, and done by one instance (in a potential multi instance setup of SMUI) using a DB lock.
    // DB lock cases:
    // ~~~~~~~~~~~~~~
    // * no lock entry exists
    // * lock entry exists:
    // ** (a) migration flagged completed
    // ** (b) migration flagged not completed (and timed out) ==> unsupported yet (as of v3.11.9)
    // TODO consider migration-retry be guarded through a minimum migration timeout, that must have been elapsed (e.g. 15min)

    // check, if there already exists a completed migration entry
    val migrationNeeded = select(migrationKey) match {
      case None => {
        logger.info(s"SmuiMigrationLock :: No migration lock found for migrationKey = $migrationKey ... executing the migration as planned!!")
        true
      }
      case Some(migrationLock) => {
        logger.info(s"SmuiMigrationLock :: Found a migration for migrationKey = $migrationKey ...")
        val wasCompleted = migrationLock.completed match {
          case None => false
          case Some(completed) => completed
        }
        if(wasCompleted) {
          logger.info(s"SmuiMigrationLock :: Migration migrationKey = $migrationKey completed ... nothing do to any more!!")
          false
        } else {
          // check for how long the migration is running
          val diffSeconds = Duration.between(
            migrationLock.lockTime,
            LocalDateTime.now()
          ).getSeconds
          logger.info(s"SmuiMigrationLock :: Migration migrationKey = $migrationKey not completed yet ... it runs for (diffSeconds = $diffSeconds)")
          if(diffSeconds > GLOBAL_MIGRATION_TIMEOUT_SECONDS) {
            logger.error(s"SmuiMigrationLock :: Migration for migrationKey = $migrationKey seems to have failed ... help me!!")
            // TODO If this happens, automatic migration (see below) might have failed, and manual interaction with the application/database or extension of the database migration logic will be necessary.
            throw new IllegalStateException(s"Database migration (migrationKey = $migrationKey) run more seconds than acceptable (diffSeconds = $diffSeconds)")
          } else {
            false
          }
        }
      }
    }

    if( migrationNeeded ) {
      // multi instance: migration needs to be lock protected on database level
      Try(
        create(migrationKey)
      ) match {
        case Success(_) => {
          logger.info(s"SmuiMigrationLock :: Successfully created lock for migrationKey = $migrationKey ... executing the migration operation!!")
          Try(
            op()
          ) match {
            case Success(_) => {
              logger.info(s"SmuiMigrationLock :: Successfully performed the migration operation (migrationKey = $migrationKey) ... will update it's state!!")
              // complete the migration transaction and - by that - release the lock
              updateCompleted(migrationKey)
            }
            case Failure(f) => {
              logger.error(s"SmuiMigrationLock :: Failed to perform the migration operation (migrationKey = $migrationKey) ... (failure = $f) help me!!")
            }
          }
        }
        case Failure(_) => {
          logger.info(s"SmuiMigrationLock :: Already found lock for migrationKey = $migrationKey ... will not execute any migration operation (within this instance/thread)!!")
        }
      }
    }
  }

}
