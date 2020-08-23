package models.eventhistory

import java.sql.Connection
import java.time.LocalDateTime

import play.api.libs.json._
import play.api.Logging
import anorm._
import anorm.SqlParser.get

import models.{Id, IdObject, SolrIndexId}
import models.input.{SearchInputId, SearchInputWithRules, SearchInput}
import models.spellings.{CanonicalSpellingId, CanonicalSpellingWithAlternatives, CanonicalSpelling}

/**
  * @see evolutions/default/6.sql
  */
object SmuiEventType extends Enumeration {
  val CREATED = Value(0)
  val UPDATED = Value(1)
  val DELETED = Value(2)
  val VIRTUALLY_CREATED = Value(3)
}

object SmuiEventSource extends Enumeration {
  val SEARCH_INPUT = "SearchInput"
  val SPELLING = "CanonicalSpelling"
}

class InputEventId(id: String) extends Id(id)
object InputEventId extends IdObject[InputEventId](new InputEventId(_))

case class InputEvent(
  id: InputEventId = InputEventId(),
  eventSource: String,
  eventType: Int,
  eventTime: LocalDateTime,
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

    // log ERROR, in case jsonPayload exceeds 5000 character limit (@see evolutions/default/6.sql)

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

  def createForSearchInput(input: SearchInputWithRules, userInfo: Option[String], virtuallyCreated: Boolean)(implicit connection: Connection): InputEvent = {
    insert(
      SmuiEventSource.SEARCH_INPUT,
      if (virtuallyCreated) SmuiEventType.VIRTUALLY_CREATED else SmuiEventType.CREATED,
      userInfo,
      input.id.id,
      Some(Json.toJson(input).toString())
    )
  }

  def updateForSearchInput(input: SearchInputWithRules, userInfo: Option[String])(implicit connection: Connection): InputEvent = {
    insert(
      SmuiEventSource.SEARCH_INPUT,
      SmuiEventType.UPDATED,
      userInfo,
      input.id.id,
      Some(Json.toJson(input).toString())
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

  def createForSpelling(input: CanonicalSpellingWithAlternatives, userInfo: Option[String], virtuallyCreated: Boolean)(implicit connection: Connection): InputEvent = {
    insert(
      SmuiEventSource.SPELLING,
      if (virtuallyCreated) SmuiEventType.VIRTUALLY_CREATED else SmuiEventType.CREATED,
      userInfo,
      input.id.id,
      Some(Json.toJson(input).toString())
    )
  }

  def updateForSpelling(input: CanonicalSpellingWithAlternatives, userInfo: Option[String])(implicit connection: Connection): InputEvent = {
    insert(
      SmuiEventSource.SPELLING,
      SmuiEventType.UPDATED,
      userInfo,
      input.id.id,
      Some(Json.toJson(input).toString())
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

    val eventPresentIds = SQL"select #${models.input.SearchInput.TABLE_NAME}.#${models.input.SearchInput.ID} from #${models.input.SearchInput.TABLE_NAME} inner join #$TABLE_NAME ON #${models.input.SearchInput.TABLE_NAME}.#${models.input.SearchInput.ID} = #$TABLE_NAME.#$INPUT_ID"
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

    // TODO inner join doesn't work with HSQLDB :-(
    val eventPresentIds = SQL"select #${models.spellings.CanonicalSpelling.TABLE_NAME}.#${models.spellings.CanonicalSpelling.ID} from #${models.spellings.CanonicalSpelling.TABLE_NAME} inner join #$TABLE_NAME on #${models.spellings.CanonicalSpelling.TABLE_NAME}.#${models.spellings.CanonicalSpelling.ID} = #$TABLE_NAME.#$INPUT_ID"
      .as(sqlIdParser.*)
      .map(sId => CanonicalSpellingId(sId))

    val allIds = SQL"select #${models.spellings.CanonicalSpelling.TABLE_NAME}.#${models.spellings.CanonicalSpelling.ID} from #${models.spellings.CanonicalSpelling.TABLE_NAME}"
      .as(sqlIdParser.*)
      .map(sId => CanonicalSpellingId(sId))

    allIds.diff(eventPresentIds)
  }

  /**
    * Determine all search_input and spelling event entities within dateFrom/To period on that SolrIndex
    *
    * @param solrIndexId
    * @param dateFrom
    * @param dateTo
    * @param connection
    * @return
    */
  // TODO consider returning List[Id]?
  // TODO write test
  def changedInputIdsForSolrIndexIdInPeriod(solrIndexId: SolrIndexId, dateFrom: LocalDateTime, dateTo: LocalDateTime)(implicit connection: Connection): List[String] = {

    // TODO make parser a general global definition for InputEvent
    val sqlEventInputIdParser: RowParser[String] = {
      get[String](s"$TABLE_NAME.$INPUT_ID")
        .map {
          case id => id
        }
    }

    def matchingInputEvents(sourceTbl: String, sourceId: String, sourceRefKey: String) = SQL(s"select $INPUT_ID from $TABLE_NAME " +
      s"join $sourceTbl on $TABLE_NAME.$INPUT_ID = $sourceTbl.$sourceId " +
      s"where $TABLE_NAME.$EVENT_TIME >= {DATE_FROM} " +
      s"and $TABLE_NAME.$EVENT_TIME <= {DATE_TO} " +
      s"and $sourceTbl.$sourceRefKey = {SOLR_INDEX_ID}")
      .on(
        'DATE_FROM -> dateFrom,
        'DATE_TO -> dateTo,
        'SOLR_INDEX_ID -> solrIndexId
      )
      .as(sqlEventInputIdParser.*)

    val matchingSearchInputEvents = matchingInputEvents(
      models.input.SearchInput.TABLE_NAME,
      models.input.SearchInput.ID,
      models.input.SearchInput.SOLR_INDEX_ID
    )

    val matchingSpellingEvents = matchingInputEvents(
      models.spellings.CanonicalSpelling.TABLE_NAME,
      models.spellings.CanonicalSpelling.ID,
      models.spellings.CanonicalSpelling.SOLR_INDEX_ID
    )

    logger.info(s":: matchingSearchInputEvents.size = ${matchingSearchInputEvents.size}")
    logger.info(s":: matchingSpellingEvents.size = ${matchingSpellingEvents.size}")

    matchingSearchInputEvents ++
    matchingSpellingEvents
  }

}