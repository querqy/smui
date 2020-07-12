package models.eventhistory

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.get
import anorm._
import models._
import models.rules._
import play.api.libs.json.{Json, OFormat}

/**
  * @see evolutions/default/5.sql
  */
object EventHistoryType extends Enumeration {
  val CREATED = Value(0)
  val UPDATED = Value(1)
  val DELETED = Value(2)
}

class SearchInputEventId(id: String) extends Id(id)
object SearchInputEventId extends IdObject[SearchInputEventId](new SearchInputEventId(_))

case class SearchInputEvent(
  id: SearchInputEventId = SearchInputEventId(),
  searchInputId: SearchInputId,
  eventType: Int,
  eventTime: LocalDateTime,
  userInfo: Option[String],
  term: Option[String],
  status: Option[Int],
  comment: Option[String],
  tagPayload: Option[String]) {

  import SearchInputEvent._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    SEARCH_INPUT_ID -> searchInputId,
    EVENT_TYPE -> eventType,
    EVENT_TIME -> eventTime,
    USER_INFO -> userInfo,
    TERM -> term,
    STATUS -> status,
    COMMENT -> comment,
    TAG_PAYLOAD -> tagPayload
  )

}

case class InputTagForLogging(
  id: InputTagId,
  property: Option[String],
  value: String
)

object InputTagForLogging {

  implicit val jsonFormat: OFormat[InputTagForLogging] = Json.format[InputTagForLogging]
}

object SearchInputEvent {

  val TABLE_NAME = "eventhistory_search_input"
  val ID = "id"
  val SEARCH_INPUT_ID = "search_input_id"
  val EVENT_TYPE = "event_type"
  val EVENT_TIME = "event_time"
  val USER_INFO = "user_info"
  val TERM = "term"
  val STATUS = "status"
  val COMMENT = "comment"
  val TAG_PAYLOAD = "tag_payload"

  val sqlParser: RowParser[SearchInputEvent] = {
    get[SearchInputEventId](s"$TABLE_NAME.$ID") ~
      get[SearchInputId](s"$TABLE_NAME.$SEARCH_INPUT_ID") ~
      get[Int](s"$TABLE_NAME.$EVENT_TYPE") ~
      get[LocalDateTime](s"$TABLE_NAME.$EVENT_TIME") ~
      get[Option[String]](s"$TABLE_NAME.$USER_INFO") ~
      get[Option[String]](s"$TABLE_NAME.$TERM") ~
      get[Option[Int]](s"$TABLE_NAME.$STATUS") ~
      get[Option[String]](s"$TABLE_NAME.$COMMENT") ~
      get[Option[String]](s"$TABLE_NAME.$TAG_PAYLOAD") map { case id ~ searchInputId ~ eventTypeRaw ~ eventTime ~ userInfo ~ term ~ status ~ comment ~ tagPayload =>
      SearchInputEvent(id, searchInputId, eventTypeRaw, eventTime, userInfo, term, status, comment, tagPayload)
    }
  }

  def toTagPayload(tags: Seq[InputTag]): String = Json.stringify(
    Json.toJson(
      tags.map(t => InputTagForLogging(t.id, t.property, t.value))
    )
  )

  def insert(searchInputId: SearchInputId, searchInputTerm: Option[String], searchInputStatus: Option[Int], searchInputComment: Option[String], oTags: Option[Seq[InputTag]], userInfo: Option[String], eventType: EventHistoryType.Value)(implicit connection: Connection): SearchInputEvent = {

    val event = SearchInputEvent(
      SearchInputEventId(),
      searchInputId,
      eventType.id,
      LocalDateTime.now(),
      userInfo,
      searchInputTerm,
      searchInputStatus,
      searchInputComment,
      oTags match {
        case None => None
        case Some(tags) => Some(toTagPayload(tags))
      }
    )
    SQL(
      s"insert into $TABLE_NAME " +
        s"($ID, $SEARCH_INPUT_ID, $EVENT_TYPE, $EVENT_TIME, $USER_INFO, $TERM, $STATUS, $COMMENT, $TAG_PAYLOAD)" +
        s" values " +
        s"({$ID}, {$SEARCH_INPUT_ID}, {$EVENT_TYPE}, {$EVENT_TIME}, {$USER_INFO}, {$TERM}, {$STATUS}, {$COMMENT}, {$TAG_PAYLOAD})"
    ).on(event.toNamedParameters: _*).execute()
    event
  }

  def updateDiff(inputBefore: SearchInputWithRules, inputAfter: SearchInputWithRules, userInfo: Option[String])(implicit connection: Connection) = {
    // determine all rule events
    val synonymEvents = RuleEvent.calcUpdateDiffForSearchInputEvent(SearchInputEventId("N/A"), inputBefore.synonymRules, inputAfter.synonymRules)
    val upDownEvents = RuleEvent.calcUpdateDiffForSearchInputEvent(SearchInputEventId("N/A"), inputBefore.upDownRules, inputAfter.upDownRules)
    val filterEvents = RuleEvent.calcUpdateDiffForSearchInputEvent(SearchInputEventId("N/A"), inputBefore.filterRules, inputAfter.filterRules)
    val deleteEvents = RuleEvent.calcUpdateDiffForSearchInputEvent(SearchInputEventId("N/A"), inputBefore.deleteRules, inputAfter.deleteRules)
    val redirectEvents = RuleEvent.calcUpdateDiffForSearchInputEvent(SearchInputEventId("N/A"), inputBefore.redirectRules, inputAfter.redirectRules)

    val allRuleEvents = synonymEvents ++ upDownEvents ++ filterEvents ++ deleteEvents ++ redirectEvents

    // diff before/after of input fields
    val diffTerm =
      if(!inputBefore.term.trim().equals(inputAfter.term.trim())) Some(inputAfter.term) else None
    val diffStatus =
      if(!inputBefore.status.equals(inputAfter.status)) Some(inputAfter.status) else None
    val diffComment =
      if(!inputBefore.comment.equals(inputAfter.comment)) Some(inputAfter.comment) else None
    val diffTagPayload =
      if(!toTagPayload(inputBefore.tags).trim().equals(toTagPayload(inputAfter.tags).trim())) Some(inputAfter.tags) else None

    def isEventEmpty(): Boolean = {
      diffTerm.isEmpty && diffStatus.isEmpty && diffComment.isEmpty && diffTagPayload.isEmpty
    }

    if(allRuleEvents.isEmpty && (!isEventEmpty())) {
      // update event only
      insert(
        inputAfter.id,
        diffTerm,
        diffStatus,
        diffComment,
        diffTagPayload,
        userInfo,
        EventHistoryType.UPDATED
      )
    } else if(!allRuleEvents.isEmpty) {
      // update events for search input & rules
      val event = insert(
        inputAfter.id,
        diffTerm,
        diffStatus,
        diffComment,
        diffTagPayload,
        userInfo,
        EventHistoryType.UPDATED
      )
      allRuleEvents.map(e => {
        RuleEvent.insert(
          RuleEvent(
            e.id,
            event.id,
            e.eventType,
            e.eventTime,
            e.term,
            e.status,
            e.ruleType,
            e.ruleId,
            e.prmPayload
          )
        )
      })
    }

  }

  private def allRules(input: SearchInputWithRules): Seq[Seq[Rule]] = {
    List(
      input.synonymRules,
      input.upDownRules,
      input.filterRules,
      input.deleteRules,
      input.redirectRules
    )
  }

  def delete(inputBefore: SearchInputWithRules, userInfo: Option[String])(implicit connection: Connection) = {
    // log search input deletion
    val event = SearchInputEvent.insert(
      inputBefore.id,
      Some(inputBefore.term),
      Some(inputBefore.status),
      Some(inputBefore.comment),
      ???,
      userInfo,
      EventHistoryType.DELETED
    )
    // log deletion of all its rules
    allRules(inputBefore).map(rules => {
      RuleEvent.deleteForSearchInputEvent(event.id, rules)
    })
  }

  def loadForSearchInput(searchInputId: SearchInputId)(implicit connection: Connection): Seq[SearchInputEvent] = {
    SQL"select * from #$TABLE_NAME where #$SEARCH_INPUT_ID = $searchInputId order by event_time desc".as(sqlParser.*)
  }

}