package models.input

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import anorm.SqlParser.get
import play.api.libs.json._

import models.{Id, IdObject, SolrIndexId}

class InputTagId(id: String) extends Id(id)
object InputTagId extends IdObject[InputTagId](new InputTagId(_))

/**
  * Defines a tag that can be assigned to a search input
  */
case class InputTag(id: InputTagId,
                    solrIndexId: Option[SolrIndexId],
                    property: Option[String],
                    value: String,
                    exported: Boolean,
                    predefined: Boolean,
                    lastUpdate: LocalDateTime) {

  import InputTag._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    SOLR_INDEX_ID -> solrIndexId,
    PROPERTY -> property,
    VALUE -> value,
    EXPORTED -> (if (exported) 1 else 0),
    PREDEFINED -> (if (predefined) 1 else 0),
    LAST_UPDATE -> lastUpdate
  )

  def tagContent = TagContent(solrIndexId, property, value)

  def displayValue: String = property.map(p => s"$p:").getOrElse("") + value

}

object InputTag {

  val TABLE_NAME = "input_tag"

  val ID = "id"
  val SOLR_INDEX_ID = "solr_index_id"
  val PROPERTY = "property"
  val VALUE = "tag_value"
  val EXPORTED = "exported"
  val PREDEFINED = "predefined"
  val LAST_UPDATE = "last_update"

  implicit val jsonReads: Reads[InputTag] = Json.reads[InputTag]

  private val defaultWrites: OWrites[InputTag] = Json.writes[InputTag]
  implicit val jsonWrites: OWrites[InputTag] = OWrites[InputTag] { tag =>
    Json.obj("displayValue" -> tag.displayValue) ++ defaultWrites.writes(tag)
  }

  def create(solrIndexId: Option[SolrIndexId],
             property: Option[String],
             value: String,
             exported: Boolean,
             predefined: Boolean = false): InputTag = {
    InputTag(InputTagId(), solrIndexId, property, value, exported, predefined, LocalDateTime.now())
  }

  val sqlParser: RowParser[InputTag] = get[InputTagId](s"$ID") ~
    get[Option[SolrIndexId]](s"$SOLR_INDEX_ID") ~
    get[Option[String]](s"$PROPERTY") ~
    get[String](s"$VALUE") ~
    get[Int](s"$EXPORTED") ~
    get[Int](s"$PREDEFINED") ~
    get[LocalDateTime](s"$LAST_UPDATE") map { case id ~ solrIndexId ~ property ~ value ~ exported ~ predefined ~ lastUpdate =>
      InputTag(id, solrIndexId, property,
        value, exported > 0, predefined > 0, lastUpdate)
  }

  def insert(tags: InputTag*)(implicit connection: Connection): Unit = {
    if (tags.nonEmpty) {
      BatchSql(s"insert into $TABLE_NAME ($ID, $SOLR_INDEX_ID, $PROPERTY, $VALUE, $EXPORTED, $PREDEFINED, $LAST_UPDATE) " +
        s"values ({$ID}, {$SOLR_INDEX_ID}, {$PROPERTY}, {$VALUE}, {$EXPORTED}, {$PREDEFINED}, {$LAST_UPDATE})",
        tags.head.toNamedParameters,
        tags.tail.map(_.toNamedParameters): _*
      ).execute()
    }
  }

  def loadAll()(implicit connection: Connection): Seq[InputTag] = {
    SQL(s"select * from $TABLE_NAME order by $PROPERTY asc, $VALUE asc")
      .as(sqlParser.*)
  }

  def deleteByIds(ids: Seq[InputTagId])(implicit connection: Connection): Unit = {
    for (idGroup <- ids.grouped(100)) {
      SQL"delete from #$TABLE_NAME where #$ID in ($idGroup)".executeUpdate()
    }
  }


}
