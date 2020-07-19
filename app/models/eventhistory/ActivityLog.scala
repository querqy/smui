package models.eventhistory

import java.time.LocalDateTime
import java.sql.Connection
import java.time.format.DateTimeFormatter

import play.api.libs.json.{Json, OFormat}
import play.api.Logging

import models.input.SearchInputWithRules
import models.rules._
import models.spellings.{CanonicalSpellingWithAlternatives, AlternativeSpelling}

/**
  *
  * @param entity
  * @param eventType
  * @param before
  * @param after
  *
  * examples below (for being displayed on the frontend):
  *
  * |  entity/eventType    |  before                 |  after              |  user info            |
  * |  ~~~~~~~~~~~~~~~~    |  ~~~~~~                 |  ~~~~~              |  ~~~~~~~~~            |
  * |  INPUT (created)     |                         |  laptop (active)    |  Paul Search Manager  |
  * |  RULE (created)      |                         |  netbook            |  Paul Search Manager  |
  * |  RULE (updated)      |  -notebück (inactive)-  |  notebook (active)  |  Paul Search Manager  |
  * |  RULE (deleted)      |  -lapptopp-             |                     |  Paul Search Manager  |
  * |  SPELL. (created)    |                         |  lapptopp (active)  |  Paul Search Manager  |
  * |  COMM. (updated)     |  -Comment before-       |  Comment after      |  Paul Search Manager  |
  */
case class DiffSummary(
  entity: String,
  eventType: String,
  before: Option[String],
  after: Option[String]
)

case class ActivityLogEntry(
  formattedDateTime: String,
  userInfo: Option[String],
  diffSummary: Seq[DiffSummary]
)

case class ActivityLog(items: Seq[ActivityLogEntry])

object ActivityLog extends Logging {

  implicit val jsonFormatDiffSummary: OFormat[DiffSummary] = Json.format[DiffSummary]
  implicit val jsonFormatActivityLogEntry: OFormat[ActivityLogEntry] = Json.format[ActivityLogEntry]
  implicit val jsonFormatActivityLog: OFormat[ActivityLog] = Json.format[ActivityLog]

  /*
   * activity/diff ecosystem of SearchInputWithRules
   */

  private def readableStatus(isActive: Boolean): String = {
    if (isActive) "activated" else "deactivated"
  }

  private def rule2term(rule: Rule): String = {
    rule match {
      case ruleWithTerm: RuleWithTerm => {
        ruleWithTerm.term
      }
      case redirectRule: RedirectRule => {
        "URL: " + redirectRule.target
      }
    }
  }

  private def readableTermStatus(term: String, status: Boolean): String = {
    term + " (" + readableStatus(status) + ")"
  }

  private def diffTermStatus(entity: String, beforeTerm: String, beforeStatus: Boolean, afterTerm: String, afterStatus: Boolean): Option[DiffSummary] = {

    val termDiff = if (beforeTerm.trim.equals(afterTerm.trim)) None else Some(afterTerm.trim)
    val statDiff = if (beforeStatus.equals(afterStatus)) None else Some(afterStatus)

    if (termDiff.isDefined || statDiff.isDefined) {

      val beforeAfter = (if (termDiff.isDefined && statDiff.isDefined) {
        (readableTermStatus(beforeTerm.trim, beforeStatus), readableTermStatus(afterTerm.trim, afterStatus))
      } else if (termDiff.isDefined) {
        (beforeTerm.trim, afterTerm.trim)
      } else { // (statDiff.isDefined)
        (readableTermStatus(beforeTerm.trim, beforeStatus), readableTermStatus(afterTerm.trim, afterStatus))
      })

      Some(
        DiffSummary(
          entity = entity,
          eventType = "updated",
          before = Some(beforeAfter._1),
          after = Some(beforeAfter._2)
        )
      )
    }
    else
      None
  }

  private def diffRules(beforeRules: Seq[Rule], afterRules: Seq[Rule]): Seq[DiffSummary] = {

    // determine CREATED/DELETED and potential UPDATED rules

    val intersectIds = beforeRules.map(_.id).intersect(afterRules.map(_.id))
    val rulesCreated = afterRules.filter(r => !intersectIds.contains(r.id))
    val rulesDeleted = beforeRules.filter(r => !intersectIds.contains(r.id))
    val rulesMaybeUpdated = beforeRules.filter(r => intersectIds.contains(r.id))

    // generate summaries for CREATED rules

    val createdSummaries = rulesCreated.map(r => DiffSummary(
      entity = "RULE",
      eventType = "created",
      before = None,
      after = Some(readableTermStatus(rule2term(r), r.isActive))
    ))

    // generate summaries for DELETED rules

    val deletedSummaries = rulesDeleted.map(r => DiffSummary(
      entity = "RULE",
      eventType = "deleted",
      before = Some(readableTermStatus(rule2term(r), r.isActive)),
      after = None
    ))

    // determine real UPDATED rules and generate summaries

    val updatedSummaries = rulesMaybeUpdated.map(beforeRule => {

      val afterRule = afterRules.filter(p => p.id.equals(beforeRule.id)).head
      diffTermStatus(
        "RULE",
        rule2term(beforeRule), beforeRule.isActive,
        rule2term(afterRule), afterRule.isActive
      )
    }).filter(d => d.isDefined)
      .map(o => o.get)

    createdSummaries ++
    deletedSummaries ++
    updatedSummaries
  }

  private def diffSearchInputEvents(beforeEvent: InputEvent, afterEvent: InputEvent): ActivityLogEntry = {

    if ((afterEvent.eventType == SmuiEventType.CREATED.id) || (afterEvent.eventType == SmuiEventType.VIRTUALLY_CREATED.id)) {

      // in case input and associations where first created (everything is new! ... meaning: is to put into "after")

      // TODO log error in case JSON read validation fails
      val afterSearchInput: SearchInputWithRules = Json.parse(afterEvent.jsonPayload.get).validate[SearchInputWithRules].asOpt.get

      val diffSummary =
        // summarise input
        List(
          DiffSummary(
            entity = "INPUT",
            eventType = "created",
            before = None,
            after = Some(readableTermStatus(afterSearchInput.term, afterSearchInput.isActive))
          )
        ) ++
        // summarise rule changes
        afterSearchInput.allRules.map(rule => {
          DiffSummary(
            entity = "RULE",
            eventType = "created",
            before = None,
            after = Some(readableTermStatus(rule2term(rule), rule.isActive))
          )
        }) ++
        // summarise comment (if present)
        (if (afterSearchInput.comment.trim.isEmpty)
          Nil
        else
          List(
            DiffSummary(
              entity = "COMMENT",
              eventType = "created",
              before = None,
              after = Some(afterSearchInput.comment.trim)
            )
          )
        )

      ActivityLogEntry(
        formattedDateTime = afterEvent.eventTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        userInfo = if(afterEvent.eventType == SmuiEventType.VIRTUALLY_CREATED.id) Some("SMUI system (pre v3.8 migration)") else afterEvent.userInfo,
        diffSummary = diffSummary
      )

    // TODO service doesnt work with last DELETED event for an input (but doesnt see those in v3.8 either)
    } else {

      // determine changes (UPDATED) of input and associated rules

      // TODO log error in case JSON read validation fails
      val beforeSearchInput: SearchInputWithRules = Json.parse(beforeEvent.jsonPayload.get).validate[SearchInputWithRules].asOpt.get
      val afterSearchInput: SearchInputWithRules = Json.parse(afterEvent.jsonPayload.get).validate[SearchInputWithRules].asOpt.get

      // diff search input (and comment)
      // case input term/status UPDATED

      // val inputDiff = termStatusDiffSummary(termDiff, statDiff) match {
      val inputDiff = diffTermStatus("INPUT", beforeSearchInput.term, beforeSearchInput.isActive, afterSearchInput.term, afterSearchInput.isActive) match {
        case Some(d: DiffSummary) => List(d)
        case None => Nil
      }

      // case comment UPDATED (or emptied)

      val commDiff = (if (beforeSearchInput.comment.trim.equals(afterSearchInput.comment.trim))
        Nil
      else
        List(
          DiffSummary(
            entity = "COMMENT",
            eventType = "updated",
            before = Some(beforeSearchInput.comment.trim),
            after = Some(afterSearchInput.comment.trim)
          )
        )
      )

      // diff rules (CREATED, UPDATED & DELETED)

      val rulesDiff = diffRules(beforeSearchInput.allRules, afterSearchInput.allRules)

      // return complete diff summary

      val diffSummary =
        inputDiff ++
        rulesDiff ++
        commDiff

      ActivityLogEntry(
        formattedDateTime = afterEvent.eventTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        userInfo = afterEvent.userInfo,
        diffSummary = diffSummary
      )
    }
  }

  /*
   * activity/diff ecosystem of CanonicalSpellingWithAlternative
   */

  private def diffAlternativeSpellings(beforeSpellings: Seq[AlternativeSpelling], afterSpellings: Seq[AlternativeSpelling]): Seq[DiffSummary] = {

    // TODO refactor to align diffAlternativeSpellings with diffRules

    val intersectIds = beforeSpellings.map(_.id).intersect(afterSpellings.map(_.id))
    val spellingsCreated = afterSpellings.filter(r => !intersectIds.contains(r.id))
    val spellingsDeleted = beforeSpellings.filter(r => !intersectIds.contains(r.id))
    val spellingsMaybeUpdated = beforeSpellings.filter(r => intersectIds.contains(r.id))

    val createdSummaries = spellingsCreated.map(s => DiffSummary(
      entity = "ALTERNATIVE",
      eventType = "created",
      before = None,
      after = Some(readableTermStatus(s.term, s.isActive))
    ))

    val deletedSummaries = spellingsDeleted.map(s => DiffSummary(
      entity = "ALTERNATIVE",
      eventType = "deleted",
      before = Some(readableTermStatus(s.term, s.isActive)),
      after = None
    ))

    val updatedSummaries = spellingsMaybeUpdated.map(beforeSpelling => {

      val afterSpelling = afterSpellings.filter(s => s.id.equals(beforeSpelling.id)).head
      diffTermStatus(
        "ALTERNATIVE",
        beforeSpelling.term, beforeSpelling.isActive,
        afterSpelling.term, afterSpelling.isActive
      )
    }).filter(d => d.isDefined)
      .map(o => o.get)

    createdSummaries ++
    deletedSummaries ++
    updatedSummaries
  }

  private def diffSpellingEvents(beforeEvent: InputEvent, afterEvent: InputEvent): ActivityLogEntry = {

    // TODO refactor to align diffSpellingEvents with diffSearchInputEvents

    if ((afterEvent.eventType == SmuiEventType.CREATED.id) || (afterEvent.eventType == SmuiEventType.VIRTUALLY_CREATED.id)) {

      // TODO log error in case JSON read validation fails
      val afterSpelling = Json.parse(afterEvent.jsonPayload.get).validate[CanonicalSpellingWithAlternatives].asOpt.get

      val diffSummary =
        // summarise canonical spellings
        List(
          DiffSummary(
            entity = "SPELLING",
            eventType = "created",
            before = None,
            after = Some(readableTermStatus(afterSpelling.term.trim, afterSpelling.isActive))
          )
        ) ++
        // summarise alternative terms
        afterSpelling.alternativeSpellings.map(alt => {
          DiffSummary(
            entity = "ALTERNATIVE",
            eventType = "created",
            before = None,
            after = Some(readableTermStatus(alt.term.trim, alt.isActive))
          )
        }) ++
        // summarise comment (if present)
        (if (afterSpelling.comment.trim.isEmpty)
          Nil
        else
          List(
            DiffSummary(
              entity = "COMMENT",
              eventType = "created",
              before = None,
              after = Some(afterSpelling.comment.trim)
            )
          )
        )

      ActivityLogEntry(
        formattedDateTime = afterEvent.eventTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        userInfo = if(afterEvent.eventType == SmuiEventType.VIRTUALLY_CREATED.id) Some("SMUI system (pre v3.8 migration)") else afterEvent.userInfo,
        diffSummary = diffSummary
      )

    } else {

      // TODO log error in case JSON read validation fails
      val beforeSpelling = Json.parse(beforeEvent.jsonPayload.get).validate[CanonicalSpellingWithAlternatives].asOpt.get
      val afterSpelling = Json.parse(afterEvent.jsonPayload.get).validate[CanonicalSpellingWithAlternatives].asOpt.get

      val spellingDiff = diffTermStatus("SPELLING", beforeSpelling.term, beforeSpelling.isActive, afterSpelling.term, afterSpelling.isActive) match {
        case Some(d: DiffSummary) => List(d)
        case None => Nil
      }

      val alternativesDiff = diffAlternativeSpellings(
        beforeSpelling.alternativeSpellings,
        afterSpelling.alternativeSpellings
      )

      val commDiff = (if (beforeSpelling.comment.trim.equals(afterSpelling.comment.trim))
        Nil
      else
        List(
          DiffSummary(
            entity = "COMMENT",
            eventType = "updated",
            before = Some(beforeSpelling.comment.trim),
            after = Some(afterSpelling.comment.trim)
          )
        )
      )

      val diffSummary =
        spellingDiff ++
        alternativesDiff ++
        commDiff

      ActivityLogEntry(
        formattedDateTime = afterEvent.eventTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        userInfo = afterEvent.userInfo,
        diffSummary = diffSummary
      )
    }
  }

  /*
   * total SMUI acivity log
   */

  private def compareInputEvents(beforeEvent: InputEvent, afterEvent: InputEvent): ActivityLogEntry = {

    // input is SearchInput vs. CanonicalSpelling

    beforeEvent.eventSource match {
      case SmuiEventSource.SEARCH_INPUT => diffSearchInputEvents(beforeEvent, afterEvent)
      case SmuiEventSource.SPELLING => diffSpellingEvents(beforeEvent, afterEvent)
      // case _ => logger.error(s"Unexpected eventSource (${beforeEvent.eventSource}) in event with id = ${beforeEvent.id}")
    }
  }

  /**
    * Interface
    *
    * @param id
    * @param connection
    * @return
    */
  def loadForId(id: String)(implicit connection: Connection): ActivityLog = {

    // get all persisted events for id
    val events = InputEvent.loadForId(id)
    if (events.isEmpty) {
      // TODO if there is not even one first CREATED event, virtually create one and reload events
      return ActivityLog(Nil)
    }
    else {

      // create new list with prepended dummy, non existent event

      // TODO make this part of InputEvent.empty()?
      val EMPTY_EVENT = InputEvent(
        id = InputEventId("--NONE--"),
        eventSource = events.head.eventSource, // !!!
        eventType = -1, // TODO add NON_EXISTENT = Value(-1) to @see models/eventhistory/InputEvent.scala :: SmuiEventType?
        eventTime = LocalDateTime.MIN,
        userInfo = None,
        inputId = "--NONE--", // semantically questionable, but the ID doesnt matter ;-)
        None
      )

      val completeEvents = EMPTY_EVENT +: events
      val pairwiseEvents = completeEvents zip completeEvents.tail

      // pairwise compare and map diffs to ActivityLog entries
      // TODO determine diff summary within day-wise time spans

      val activityLogItems = pairwiseEvents.map( eventPair => {
        compareInputEvents(eventPair._1, eventPair._2)
      })

      ActivityLog(
        activityLogItems
          .reverse
          .filter(entry => !entry.diffSummary.isEmpty)
      )
    }
  }

}
