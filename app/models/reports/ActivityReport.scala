package models.reports

import java.sql.Connection
import java.time.LocalDateTime

import play.api.libs.json.{Json, OFormat}
import play.api.Logging

import models.SolrIndexId
import models.eventhistory.{ActivityLog}

case class ActivityReportEntry(
  // TODO make LocalDateTime and to conversion in JSON transfer
  modificationTime: String,
  user: Option[String],
  inputTerm: String,
  entity: String,
  eventType: String,
  before: Option[String],
  after: Option[String]
)

case class ActivityReport(
  items: Seq[ActivityReportEntry]
)

object ActivityReport extends Logging {

  implicit val jsonFormatActivityReportEntry: OFormat[ActivityReportEntry] = Json.format[ActivityReportEntry]
  implicit val jsonFormatActivityReport: OFormat[ActivityReport] = Json.format[ActivityReport]

  /**
    * Interface
    */

  def reportForSolrIndexIdInPeriod(solrIndexId: SolrIndexId, dateFrom: LocalDateTime, dateTo: LocalDateTime)(implicit connection: Connection): ActivityReport =
    ActivityReport(
      items = ActivityLog.changesForSolrIndexInPeriod(solrIndexId, dateFrom, dateTo).items
        .flatMap(logEntry => {
          logEntry.diffSummary
            .map(diffSummary => {
              ActivityReportEntry(
                modificationTime = logEntry.formattedDateTime,
                user = logEntry.userInfo,
                inputTerm = diffSummary.inputTerm.get,
                entity = diffSummary.entity,
                eventType = diffSummary.eventType,
                before = diffSummary.before,
                after = diffSummary.after
              )
            })
        })
    )

}


