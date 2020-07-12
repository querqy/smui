package models.eventhistory

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.get
import anorm._
import models.rules._
import models.{Id, IdObject}

class RuleEventId(id: String) extends Id(id)
object RuleEventId extends IdObject[RuleEventId](new RuleEventId(_))

case class RuleEvent(
  id: RuleEventId = RuleEventId(),
  inputEventId: SearchInputEventId,
  eventType: Int,
  eventTime: LocalDateTime,
  term: Option[String],
  status: Option[Int],
  ruleType: String,
  ruleId: String,
  prmPayload: Option[String]) {

  import RuleEvent._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    INPUT_EVENT_ID -> inputEventId,
    EVENT_TYPE -> eventType,
    EVENT_TIME -> eventTime,
    TERM -> term,
    STATUS -> status,
    RULE_TYPE -> ruleType,
    RULE_ID -> ruleId,
    PRM_PAYLOAD -> prmPayload
  )

}

object RuleEvent {

  val TABLE_NAME = "eventhistory_rule"
  val ID = "id"
  val INPUT_EVENT_ID = "input_event_id"
  val EVENT_TYPE = "event_type"
  val EVENT_TIME = "event_time"
  val TERM = "term"
  val STATUS = "status"
  val RULE_TYPE = "rule_type"
  val RULE_ID = "rule_id"
  val PRM_PAYLOAD = "prm_payload"

  val sqlParser: RowParser[RuleEvent] = {
    get[RuleEventId](s"$TABLE_NAME.$ID") ~
      get[SearchInputEventId](s"$TABLE_NAME.$INPUT_EVENT_ID") ~
      get[Int](s"$TABLE_NAME.$EVENT_TYPE") ~
      get[LocalDateTime](s"$TABLE_NAME.$EVENT_TIME") ~
      get[Option[String]](s"$TABLE_NAME.$TERM") ~
      get[Option[Int]](s"$TABLE_NAME.$STATUS") ~
      get[String](s"$TABLE_NAME.$RULE_TYPE") ~
      get[String](s"$TABLE_NAME.$RULE_ID") ~
      get[Option[String]](s"$TABLE_NAME.$PRM_PAYLOAD") map { case id ~ eventId ~ eventTypeRaw ~ eventTime ~ term ~ status ~ ruleType ~ ruleId ~ prmPayload =>
      RuleEvent(id, eventId, eventTypeRaw, eventTime, term, status, ruleType, ruleId, prmPayload)
    }
  }

  def insert(event: RuleEvent)(implicit connection: Connection): RuleEvent = {

    SQL(
      s"insert into $TABLE_NAME " +
        s"($ID, $INPUT_EVENT_ID, $EVENT_TYPE, $EVENT_TIME, $TERM, $STATUS, $RULE_TYPE, $RULE_ID, $PRM_PAYLOAD)" +
        s" values " +
        s"({$ID}, {$INPUT_EVENT_ID}, {$EVENT_TYPE}, {$EVENT_TIME}, {$TERM}, {$STATUS}, {$RULE_TYPE}, {$RULE_ID}, {$PRM_PAYLOAD})"
    ).on(event.toNamedParameters: _*).execute()
    event
  }

  def calcUpdateDiffForSearchInputEvent(eventId: SearchInputEventId, beforeRules: Seq[Rule], afterRules: Seq[Rule])(implicit connection: Connection) = {

    val intersectIds = beforeRules.map(_.id).intersect(afterRules.map(_.id))
    // log all rules bering created
    val rulesCreated = afterRules.filter(r => !intersectIds.contains(r.id)).map(r => {
      RuleEvent(
        RuleEventId(),
        eventId,
        EventHistoryType.CREATED.id,
        LocalDateTime.now(),
        Some(r.termForLog),
        Some(r.status),
        r.ruleType,
        r.id.id,
        ???
      )
    })
    // log all rules being deleted
    val rulesDeleted = beforeRules.filter(r => !intersectIds.contains(r.id)).map(r => {
      RuleEvent(
        RuleEventId(),
        eventId,
        EventHistoryType.DELETED.id,
        LocalDateTime.now(),
        Some(r.termForLog),
        Some(r.status),
        r.ruleType,
        r.id.id,
        ???
      )
    })
    // log all rules being updated
    val rulesUpdated = beforeRules.filter(r1 => intersectIds.contains(r1.id)).map(r1 => {
      val r2 = afterRules.filter(r2 => r2.id.equals(r1.id)).head
      val diffTerm = if (!r1.termForLog.trim().equals(r2.termForLog.trim())) Some(r1.termForLog) else None
      val diffStatus = if (!r1.status.equals(r2.status)) Some(r1.status) else None
      val diffPayload = ???
      RuleEvent(
        RuleEventId(),
        eventId,
        EventHistoryType.UPDATED.id,
        LocalDateTime.now(),
        diffTerm,
        diffStatus,
        ???, // r1.ruleType,
        ???, // r1.id.id,
        diffPayload
      )
    })

    // return concatenated list of rule events
    rulesCreated ++ rulesDeleted ++ rulesUpdated
  }

  def deleteForSearchInputEvent(eventId: SearchInputEventId, beforeRules: Seq[Rule])(implicit connection: Connection) = {
    beforeRules.map(r => {
      insert(
        RuleEvent(
          RuleEventId(),
          eventId,
          EventHistoryType.DELETED.id,
          LocalDateTime.now(),
          Some(r.termForLog),
          Some(r.status),
          r.ruleType,
          r.id.id,
          ???
        )
      )
    })
  }

  def loadForSearchInputEvent(eventId: SearchInputEventId)(implicit connection: Connection): Seq[RuleEvent] = {
    SQL"select * from #$TABLE_NAME where #$INPUT_EVENT_ID = $eventId order by event_time desc".as(sqlParser.*)
  }

}
