package models.eventhistory

import java.sql.Connection
import java.time.LocalDateTime

import play.api.libs.json._
import play.api.Logging
import anorm._
import anorm.SqlParser.get

import models.{Id, IdObject, SolrIndexId}
import models.input.{SearchInputId, FullSearchInputWithRules}
import models.spellings.{CanonicalSpellingId, FullCanonicalSpellingWithAlternatives}

/**
  * @see conf/evolutions/default/6.sql
  */
object SmuiEventType extends Enumeration {
  val CREATED = Value(0)
  val UPDATED = Value(1)
  val DELETED = Value(2)
  val VIRTUALLY_CREATED = Value(3)

  // TODO maybe there is a more elegant option?
  def toSmuiEventType(rawEventType: Int) = rawEventType match {
    case 0 => CREATED
    case 1 => UPDATED
    case 2 => DELETED
    case 3 => VIRTUALLY_CREATED
    // case _ => TODO throw an IllegalState exception
  }
}

object SmuiEventSource extends Enumeration {
  val SEARCH_INPUT = "SearchInput"
  val SPELLING = "CanonicalSpelling"
}

class InputEventId(id: String) extends Id(id)
object InputEventId extends IdObject[InputEventId](new InputEventId(_))

// TODO consider to introduce an event number sequence to trace event order (regardless the eventTime)
case class InputEvent(
  id: InputEventId = InputEventId(),
  eventSource: String,
  eventType: Int,
  eventTime: LocalDateTime,
  // TODO Make userInfo a mandatory information, once unauthenticated SMUI usage is not possible any more >v3.14 (see https://github.com/querqy/smui/pull/83#issuecomment-1023284550). Also adjust persistence: `user_info varchar(100) not null` (see conf/evolutions/default/6.sql).
  userInfo: Option[String],
  inputId: String,
  jsonPayload: Option[String]) {

  import InputEvent._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    EVENT_SOURCE -> eventSource,
    EVENT_TYPE -> eventType,
    EVENT_TIME -> eventTime,
    USER_INFO -> userInfo,
    INPUT_ID -> inputId,
    JSON_PAYLOAD -> jsonPayload
  )

}

object InputEvent extends Logging {

  val TABLE_NAME = "input_event"
  val ID = "id"
  val EVENT_SOURCE = "event_source"
  val EVENT_TYPE = "event_type"
  val EVENT_TIME = "event_time"
  val USER_INFO = "user_info"
  val INPUT_ID = "input_id"
  val JSON_PAYLOAD = "json_payload"

  val sqlParser: RowParser[InputEvent] = {
    get[InputEventId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$EVENT_SOURCE") ~
      get[Int](s"$TABLE_NAME.$EVENT_TYPE") ~
      get[LocalDateTime](s"$TABLE_NAME.$EVENT_TIME") ~
      get[Option[String]](s"$TABLE_NAME.$USER_INFO") ~
      get[String](s"$TABLE_NAME.$INPUT_ID") ~
      get[Option[String]](s"$TABLE_NAME.$JSON_PAYLOAD") map {
        case id ~ eventSource ~ eventTypeRaw ~ eventTime ~ userInfo ~ inputId ~ jsonPayload =>
          InputEvent(id, eventSource, eventTypeRaw, eventTime, userInfo, inputId, jsonPayload)
    }
  }

  private def insert(eventSource: String, eventType: SmuiEventType.Value, userInfo: Option[String], inputId: String, jsonPayload: Option[String])(implicit connection: Connection): InputEvent = {

    // log ERROR, in case jsonPayload exceeds 10000 character limit (@see evolutions/default/6.sql)
    jsonPayload.map(payload => {
      if (payload.size > 10000) {
        logger.error(s"jsonPayload for inputId = $inputId (eventSource = $eventSource) exceeds 10K (payload.size = ${payload.size}), INSERT will fail!!")
      }
    })

    val event = InputEvent(
      InputEventId(),
      eventSource,
      eventType.id,
      LocalDateTime.now(),
      userInfo,
      inputId,
      jsonPayload
    )
    SQL(
      s"insert into $TABLE_NAME " +
        s"($ID, $EVENT_SOURCE, $EVENT_TYPE, $EVENT_TIME, $USER_INFO, $INPUT_ID, $JSON_PAYLOAD)" +
        s" values " +
        s"({$ID}, {$EVENT_SOURCE}, {$EVENT_TYPE}, {$EVENT_TIME}, {$USER_INFO}, {$INPUT_ID}, {$JSON_PAYLOAD})"
    ).on(event.toNamedParameters: _*).execute()
    event
  }

  /*
   * CRUD events for SearchInputWithRules
   */

  def createForSearchInput(inputId: SearchInputId, userInfo: Option[String], virtuallyCreated: Boolean)(implicit connection: Connection): InputEvent = {
    val fullInput = FullSearchInputWithRules.loadById(inputId).get
    insert(
      SmuiEventSource.SEARCH_INPUT,
      if (virtuallyCreated) SmuiEventType.VIRTUALLY_CREATED else SmuiEventType.CREATED,
      userInfo,
      fullInput.id.id,
      Some(Json.toJson(fullInput).toString())
    )
  }

  def updateForSearchInput(inputId: SearchInputId, userInfo: Option[String])(implicit connection: Connection): InputEvent = {
    val fullInput = FullSearchInputWithRules.loadById(inputId).get
    insert(
      SmuiEventSource.SEARCH_INPUT,
      SmuiEventType.UPDATED,
      userInfo,
      fullInput.id.id,
      Some(Json.toJson(fullInput).toString())
    )
  }

  def deleteForSearchInput(inputId: SearchInputId, userInfo: Option[String])(implicit connection: Connection): InputEvent = {

    // write event for deletion of search input
    insert(
      SmuiEventSource.SEARCH_INPUT,
      SmuiEventType.DELETED,
      userInfo,
      inputId.id,
      None
    )
  }

  /*
   * CRUD events for CanonicalSpellingWithAlternatives
   */
  // TODO think about generalising belows logic for CanonicalSpellingWithAlternatives with the one above for SearchInputWithRules

  def createForSpelling(inputId: CanonicalSpellingId, userInfo: Option[String], virtuallyCreated: Boolean)(implicit connection: Connection): InputEvent = {
    val fullSpelling = FullCanonicalSpellingWithAlternatives.loadById(inputId).get
    insert(
      SmuiEventSource.SPELLING,
      if (virtuallyCreated) SmuiEventType.VIRTUALLY_CREATED else SmuiEventType.CREATED,
      userInfo,
      fullSpelling.id.id,
      Some(Json.toJson(fullSpelling).toString())
    )
  }

  def updateForSpelling(inputId: CanonicalSpellingId, userInfo: Option[String])(implicit connection: Connection): InputEvent = {
    val fullSpelling = FullCanonicalSpellingWithAlternatives.loadById(inputId).get
    insert(
      SmuiEventSource.SPELLING,
      SmuiEventType.UPDATED,
      userInfo,
      fullSpelling.id.id,
      Some(Json.toJson(fullSpelling).toString())
    )
  }

  def deleteForSpelling(inputId: CanonicalSpellingId, userInfo: Option[String])(implicit connection: Connection): InputEvent = {

    // write event for deletion of search input
    insert(
      SmuiEventSource.SPELLING,
      SmuiEventType.DELETED,
      userInfo,
      inputId.id,
      None
    )
  }

  /**
    *
    * @param inputId either be a SearchInput or CanonicalSpelling ID.
    * @param connection
    * @return
    */
  def loadForId(inputId: String)(implicit connection: Connection): Seq[InputEvent] = {
    SQL"select * from #$TABLE_NAME where #$INPUT_ID = $inputId order by event_time asc".as(sqlParser.*)
  }

  /*
   * Interface to determine which inputs entities do not have an event history yet (for pre v3.8 migration)
   * @see models/eventhistory/MigrationService.scala
   */

  def searchInputIdsWithoutEvent()(implicit connection: Connection): Seq[SearchInputId] = {

    // TODO make parser a general global definition for InputEvent
    val sqlIdParser: RowParser[String] = {
      get[String](s"${models.input.SearchInput.TABLE_NAME}.${models.input.SearchInput.ID}")
        .map {
          case id => id
        }
    }

    val eventPresentIds = SQL"select #${models.input.SearchInput.TABLE_NAME}.#${models.input.SearchInput.ID} from #${models.input.SearchInput.TABLE_NAME} join #$TABLE_NAME ON #${models.input.SearchInput.TABLE_NAME}.#${models.input.SearchInput.ID} = #$TABLE_NAME.#$INPUT_ID"
      .as(sqlIdParser.*)
      .map(sId => SearchInputId(sId))

    val allIds = SQL"select #${models.input.SearchInput.TABLE_NAME}.#${models.input.SearchInput.ID} from #${models.input.SearchInput.TABLE_NAME}"
      .as(sqlIdParser.*)
      .map(sId => SearchInputId(sId))

    allIds.diff(eventPresentIds)
  }

  // TODO think about generalising belows logic for CanonicalSpellingWithAlternatives with the one above for SearchInputWithRules
  def spellingIdsWithoutEvent()(implicit connection: Connection): Seq[CanonicalSpellingId] = {

    // TODO make parser a general global definition for InputEvent
    val sqlIdParser: RowParser[String] = {
      get[String](s"${models.spellings.CanonicalSpelling.TABLE_NAME}.${models.spellings.CanonicalSpelling.ID}")
        .map {
          case id => id
        }
    }

    val eventPresentIds = SQL"select #${models.spellings.CanonicalSpelling.TABLE_NAME}.#${models.spellings.CanonicalSpelling.ID} from #${models.spellings.CanonicalSpelling.TABLE_NAME} join #$TABLE_NAME on #${models.spellings.CanonicalSpelling.TABLE_NAME}.#${models.spellings.CanonicalSpelling.ID} = #$TABLE_NAME.#$INPUT_ID"
      .as(sqlIdParser.*)
      .map(sId => CanonicalSpellingId(sId))

    val allIds = SQL"select #${models.spellings.CanonicalSpelling.TABLE_NAME}.#${models.spellings.CanonicalSpelling.ID} from #${models.spellings.CanonicalSpelling.TABLE_NAME}"
      .as(sqlIdParser.*)
      .map(sId => CanonicalSpellingId(sId))

    allIds.diff(eventPresentIds)
  }

  /**
    * Determine all search_input and spelling entity IDs with change event within dateFrom/To period on that SolrIndex
    *
    * @param solrIndexId
    * @param dateFrom
    * @param dateTo
    * @param connection
    * @return
    */
  // TODO consider returning List[Id]?
  // TODO write test
  // TODO maybe merge implementations of changedInputIdsForSolrIndexIdInPeriod() and changeEventsForIdInPeriod() (below) to reduce amount of SQL requests against database ==> performance
  def allChangedInputIdsForSolrIndexIdInPeriod(solrIndexId: SolrIndexId, dateFrom: LocalDateTime, dateTo: LocalDateTime)(implicit connection: Connection): List[String] = {

    val allChangeEvents = SQL(
      s"select * from $TABLE_NAME " +
      s"where $EVENT_TIME >= {dateFrom} " +
      s"and $EVENT_TIME <= {dateTo} " +
      s"order by $EVENT_TIME asc"
    )
      .on(
        'dateFrom -> dateFrom,
        'dateTo -> dateTo
      )
      .as(sqlParser.*)

    def isEventForSolrIndex(inputEvent: InputEvent): Boolean = {

      if (!SmuiEventType.toSmuiEventType(inputEvent.eventType).equals(SmuiEventType.DELETED)) {

        inputEvent.eventSource match {
          case SmuiEventSource.SEARCH_INPUT => {
            // TODO log error in case JSON read validation fails
            val searchInput = Json.parse(inputEvent.jsonPayload.get).validate[FullSearchInputWithRules].asOpt.get
            searchInput.solrIndexId.equals(solrIndexId)
          }
          case SmuiEventSource.SPELLING => {
            // TODO log error in case JSON read validation fails
            val spelling = Json.parse(inputEvent.jsonPayload.get).validate[FullCanonicalSpellingWithAlternatives].asOpt.get
            spelling.solrIndexId.equals(solrIndexId)
          }
        }

      } else {

        // seek first instant previous event for input and verify solrIndexId
        val beforeEvents = SQL(
          s"select * from $TABLE_NAME " +
          s"where $INPUT_ID = {inputId} " +
          s"and $EVENT_TIME < {dateFrom} " +
          s"order by $EVENT_TIME desc " +
          s"limit 1"
        )
          .on(
            'inputId -> inputEvent.inputId,
            'dateFrom -> inputEvent.eventTime,
          )
          .as(sqlParser.*)

        isEventForSolrIndex(beforeEvents.head)

      }
    }

    allChangeEvents
      .filter(e => isEventForSolrIndex(e))
      .map(e => e.inputId)
      .distinct

  }

  /**
    * Determine the event pair, that describes the change of an entity within a given period (if any happened)
    *
    * @param inputId
    * @param dateFrom
    * @param dateTo
    * @param connection
    * @return
    */
  def changeEventsForIdInPeriod(inputId: String, dateFrom: LocalDateTime, dateTo: LocalDateTime)(implicit connection: Connection): (Option[InputEvent], Option[InputEvent]) = {

    val allChangeEvents = SQL(
      s"select * from $TABLE_NAME " +
      s"where $INPUT_ID = {inputId} " +
      s"and $EVENT_TIME >= {dateFrom} " +
      s"and $EVENT_TIME <= {dateTo} " +
      s"order by $EVENT_TIME asc"
    )
      .on(
        'inputId -> inputId,
        'dateFrom -> dateFrom,
        'dateTo -> dateTo
      )
      .as(sqlParser.*)

    if(allChangeEvents.isEmpty) {
      // No change can be detected for input (ID) in period
      (None, None)
    } else if(allChangeEvents.size == 1) {
      // find the first event before dateFrom
      val beforeEvents = SQL(
        s"select * from $TABLE_NAME " +
        s"where $INPUT_ID = {inputId} " +
        s"and $EVENT_TIME < {dateFrom} " +
        s"order by $EVENT_TIME desc " +
        s"limit 1"
      )
        .on(
          'inputId -> inputId,
          'dateFrom -> dateFrom,
        )
        .as(sqlParser.*)

      if(beforeEvents.isEmpty) {
        (None, Some(allChangeEvents.head))
      } else {
        (Some(beforeEvents.head), Some(allChangeEvents.head))
      }
    } else {
      (Some(allChangeEvents.head), Some(allChangeEvents.last))
    }
  }

  def countEventsWithoutProperUserInfo(implicit connection: Connection): Long = {

//    val sqlIntParser: RowParser[Int] = {
//      get[Int](s"") map {
//        case i => i
//      }
//    }

    val countEvents = SQL(
      s"select count(*) from $TABLE_NAME " +
        s"where $USER_INFO is null"
    )
      .as(anorm.SqlParser.scalar[Long].single)

    countEvents
  }

}
