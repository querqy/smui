package models.reports

import java.time.LocalDateTime

import play.api.Logging
import play.api.libs.json.{Json, OFormat}

import models.SolrIndexId
import models.eventhistory.ActivityLogEntry

case class ActivityReport(items: Seq[ActivityLogEntry])

object ActivityReport extends Logging {

  // TODO ActivityLog implicit JSON converter needed?
  import models.eventhistory.ActivityLog._
  implicit val jsonFormatActivityReport: OFormat[ActivityReport] = Json.format[ActivityReport]

  /**
    * Interface
    *
    * @param solrIndexId
    * @param dateFrom
    * @param dateTo
    */
  def loadForSolrIndexIdInPeriod(solrIndexId: SolrIndexId, dateFrom: LocalDateTime, dateTo: LocalDateTime) = {
    ActivityReport(
      items = Nil
    )
  }

}
