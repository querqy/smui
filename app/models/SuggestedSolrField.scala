package models

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.get
import anorm._
import play.api.libs.json.{Json, OFormat}

class SuggestedSolrFieldId(id: String) extends Id(id)
object SuggestedSolrFieldId extends IdObject[SuggestedSolrFieldId](new SuggestedSolrFieldId(_))


case class SuggestedSolrField(id: SuggestedSolrFieldId = SuggestedSolrFieldId(),
                         name: String) {

}

object SuggestedSolrField {

  implicit val jsonFormat: OFormat[SuggestedSolrField] = Json.format[SuggestedSolrField]

  val TABLE_NAME = "suggested_solr_field"
  val ID = "id"
  val NAME = "name"
  val SOLR_INDEX_ID = "solr_index_id"
  val LAST_UPDATE = "last_update"

  val sqlParser: RowParser[SuggestedSolrField] = {
    get[SuggestedSolrFieldId](s"$ID") ~
      get[String](s"$NAME") map { case id ~ name =>
      SuggestedSolrField(id, name)
    }
  }

  def listAll(solrIndexId: SolrIndexId)(implicit connection: Connection): List[SuggestedSolrField] = {
    SQL"select * from #$TABLE_NAME where #$SOLR_INDEX_ID = $solrIndexId order by #$NAME asc".as(sqlParser.*)
  }

  def insert(solrIndexId: SolrIndexId, fieldName: String)(implicit connection: Connection): SuggestedSolrField = {
    val field = SuggestedSolrField(SuggestedSolrFieldId(), fieldName)
    SQL(s"insert into $TABLE_NAME($ID, $NAME, $SOLR_INDEX_ID, $LAST_UPDATE) values ({$ID}, {$NAME}, {$SOLR_INDEX_ID}, {$LAST_UPDATE})")
      .on(
        ID -> field.id,
        NAME -> fieldName,
        SOLR_INDEX_ID -> solrIndexId,
        LAST_UPDATE -> LocalDateTime.now()
      )
      .execute()
    field
  }


}