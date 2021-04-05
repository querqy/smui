package models.reports

import java.sql.Connection
import java.time.LocalDateTime

import play.api.Logging

import anorm._
import anorm.SqlParser.get

case class DeploymentLog(id: String, lastUpdate: LocalDateTime, result: Int)

// TODO add test
object DeploymentLog extends Logging {

  val sqlParserDeploymentLogDetail: RowParser[DeploymentLog] = {
    get[String](s"id") ~
      get[LocalDateTime](s"last_update") ~
      get[Int](s"result") map { case id ~ lastUpdate ~ result =>
      DeploymentLog(id, lastUpdate, result)
    }
  }

  def loadForSolrIndexIdAndPlatform(solrIndexId: String, targetPlatform: String)(implicit connection: Connection): Option[DeploymentLog] = {
    SQL"select * from deployment_log where solr_index_id = $solrIndexId and target_platform = $targetPlatform order by last_update desc".as(sqlParserDeploymentLogDetail.*).headOption
  }

}
