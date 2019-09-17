package models

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.get
import anorm._

class SearchInputId(id: String) extends Id(id)
object SearchInputId extends IdObject[SearchInputId](new SearchInputId(_))

case class SearchInput(id: SearchInputId = SearchInputId(),
                       solrIndexId: SolrIndexId,
                       term: String,
                       lastUpdate: LocalDateTime) {

  import SearchInput._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    SOLR_INDEX_ID -> solrIndexId,
    TERM -> term,
    LAST_UPDATE -> lastUpdate
  )


}

object SearchInput {

  val TABLE_NAME = "search_input"
  val ID = "id"
  val TERM = "term"
  val SOLR_INDEX_ID = "solr_index_id"
  val LAST_UPDATE = "last_update"

  val sqlParser: RowParser[SearchInput] = {
    get[SearchInputId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[SolrIndexId](s"$TABLE_NAME.$SOLR_INDEX_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ term ~ indexId ~ lastUpdate =>
        SearchInput(id, indexId, term, lastUpdate)
    }
  }

  def insert(solrIndexId: SolrIndexId, term: String)(implicit connection: Connection): SearchInput = {
    val input = SearchInput(SearchInputId(), solrIndexId, term, LocalDateTime.now())
    SQL(s"insert into $TABLE_NAME ($ID, $TERM, $SOLR_INDEX_ID, $LAST_UPDATE) values ({$ID}, {$TERM}, {$SOLR_INDEX_ID}, {$LAST_UPDATE})")
      .on(input.toNamedParameters: _*).execute()
    input
  }

  def loadAllForIndex(solrIndexId: SolrIndexId)(implicit connection: Connection): List[SearchInput] = {
    SQL"select * from #$TABLE_NAME where #$SOLR_INDEX_ID = $solrIndexId order by #$TERM asc".as(sqlParser.*)
  }

  def loadAllIdsForIndex(solrIndexId: SolrIndexId)(implicit connection: Connection): List[SearchInputId] = {
    SQL"select #$ID from #$TABLE_NAME where #$SOLR_INDEX_ID = $solrIndexId order by #$TERM asc".as(get[SearchInputId](ID).*)
  }

  def loadById(id: SearchInputId)(implicit connection: Connection): Option[SearchInput] = {
    SQL"select * from #$TABLE_NAME where #$ID = $id".as(sqlParser.*).headOption
  }

  def update(id: SearchInputId, term: String)(implicit connection: Connection): Unit = {
    SQL"update #$TABLE_NAME set #$TERM = $term, #$LAST_UPDATE = ${LocalDateTime.now()} where #$ID = $id".executeUpdate()
  }

  /**
    * Deletes the searchInput itself, not any rules belonging to it.
    * For also deleting rules, use SearchInputWithRules.delete
    */
  def delete(id: SearchInputId)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME where #$ID = $id".executeUpdate()
  }


}
