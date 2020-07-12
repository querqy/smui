package models.eventhistory

import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import models.eventhistory._
import models.{SearchInput, SearchInputId}
import play.api.libs.json.{Json, OFormat}

case class ActivityLogEntry(
  dateTime: String,
  userInfo: Option[String],
  inputSummary: String,
  rulesSummary: Option[String],
  commentSummary: Option[String]
)

/**
  * Constructing a readable / shortened version of the input and rules events.
  * ~~~
  *
  * For SearchInputEvent, e.g.:
  * "5.1 Soundsystem" created.
  * "5.1 Soundsystem" updated, deactivated.
  * (deletion of rules is not displayed)
  *
  * For RuleEvent, e.g.:
  * TODO
  */
object InputRuleActivityLog {

  implicit val jsonFormat: OFormat[ActivityLogEntry] = Json.format[ActivityLogEntry]

  private def isSearchInputEventEmpty(e: SearchInputEvent): Boolean = {
    e.term.isEmpty && e.status.isEmpty && e.comment.isEmpty // TODO tagPayload not considered for now
  }

  private def isRuleEventEmpty(e: RuleEvent): Boolean = {
    e.term.isEmpty // TODO status and prmPayload not considered for now
  }

  def mapEventType(eventType: EventHistoryType.Value): String = {
    eventType match {
      case EventHistoryType.CREATED => "created"
      case EventHistoryType.UPDATED => "updated"
      case EventHistoryType.DELETED => "deleted"
    }
  }

  private def readableRuleEventsChange(ruleEvents: Seq[RuleEvent]): Option[String] = {

    def nonEmptySentence(cmpEventType: EventHistoryType.Value): String = {
      val retSentence = ruleEvents.filter(_.eventType.equals(cmpEventType.id)).map("'" + _.term.get + "'").mkString(", ")
      if(!retSentence.isEmpty) {
        s"$retSentence ${mapEventType(cmpEventType)}"
      } else {
        ""
      }
    }

    val listSummary = List[String](
      nonEmptySentence(EventHistoryType.CREATED),
      nonEmptySentence(EventHistoryType.UPDATED),
      nonEmptySentence(EventHistoryType.DELETED),
    ).filter(!_.isEmpty)

    if(!listSummary.isEmpty) Some(listSummary.mkString("; ")) else None
  }

  def loadBySearchInputId(searchInputId: SearchInputId)(implicit connection: Connection): Seq[ActivityLogEntry] = {
    val searchInput = SearchInput.loadById(searchInputId).get
    val events = SearchInputEvent
      .loadForSearchInput(searchInputId)
      .map(inputEvent => {
        (inputEvent, RuleEvent.loadForSearchInputEvent(inputEvent.id).filter(!isRuleEventEmpty(_)))
      })
      .filter(eventDict => {
        val inputEvent = eventDict._1
        val ruleEvents = eventDict._2
        (!isSearchInputEventEmpty(inputEvent)) || (!ruleEvents.isEmpty)
      })

    val inputRuleActivityLog = events.map(eventDict => {

      val inputEvent = eventDict._1
      val ruleEvents = eventDict._2

      def readableActivityReference(): String = {
        if(isSearchInputEventEmpty(inputEvent)) {
          s"Rules for '${searchInput.term}'"
        } else {
          if (inputEvent.term.isEmpty) {
            s"'${searchInput.term}'"
          } else {
            s"Input term '${inputEvent.term.get}'"
          }
        }
      }

      def readableEventType(): String = {
        mapEventType(EventHistoryType(inputEvent.eventType))
      }

      def readableStatusChange(): String = {
        if(EventHistoryType(inputEvent.eventType).equals(EventHistoryType.UPDATED)) {
          inputEvent.status match {
            case Some(status) => {
              status match {
                case 0x00 => ", activated"
                case 0x01 => ", deactivated"
              }
            }
            case None => {
              ""
            }
          }
        } else {
          ""
        }
      }

      val dateTime = inputEvent.eventTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      val userInfo = inputEvent.userInfo
      val inputSummary = s"${readableActivityReference()} ${readableEventType()}${readableStatusChange()}."
      val rulesSummary = readableRuleEventsChange(ruleEvents)
      val commentSummary = inputEvent.comment match {
        case Some(commentStr) => if(commentStr.isEmpty) None else Some(s"$commentStr (updated)")
        case None => None
      }

      ActivityLogEntry(
        dateTime,
        userInfo,
        inputSummary,
        rulesSummary,
        commentSummary
      )
    })

    inputRuleActivityLog
  }

}
