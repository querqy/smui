package models.input

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.get
import anorm._

case class TagInputAssociation(tagId: InputTagId,
                               searchInputId: SearchInputId,
                               lastUpdate: LocalDateTime = LocalDateTime.now()) {

  import TagInputAssociation._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    TAG_ID -> tagId,
    INPUT_ID -> searchInputId,
    LAST_UPDATE -> lastUpdate
  )

}

object TagInputAssociation {

  val TABLE_NAME = "tag_2_input"

  val TAG_ID = "tag_id"
  val INPUT_ID = "input_id"
  val LAST_UPDATE = "last_update"

  def insert(associations: TagInputAssociation*)(implicit connection: Connection): Unit = {
    if (associations.nonEmpty) {
      BatchSql(s"insert into $TABLE_NAME ($TAG_ID, $INPUT_ID, $LAST_UPDATE) " +
        s"values ({$TAG_ID}, {$INPUT_ID}, {$LAST_UPDATE})",
        associations.head.toNamedParameters,
        associations.tail.map(_.toNamedParameters): _*
      ).execute()
    }
  }

  /**
    * Deletes old associations for the given search input and inserts new ones with the given tag IDs.
    */
  def updateTagsForSearchInput(searchInputId: SearchInputId, tagIds: Seq[InputTagId])(implicit connection: Connection): Unit = {
    deleteBySearchInputId(searchInputId)
    insert(tagIds.map(tagId => TagInputAssociation(tagId, searchInputId)): _*)
  }

  def loadTagsBySearchInputId(id: SearchInputId)(implicit connection: Connection): Seq[InputTag] = {
    SQL(s"select * from $TABLE_NAME a, ${InputTag.TABLE_NAME} t where a.$INPUT_ID = {inputId} " +
      s"and a.$TAG_ID = t.${InputTag.ID} order by t.${InputTag.PROPERTY} asc, t.${InputTag.VALUE} asc").
      on("inputId" -> id).as(InputTag.sqlParser.*)
  }

  def loadTagsBySearchInputIds(ids: Seq[SearchInputId])(implicit connection: Connection): Map[SearchInputId, Seq[InputTag]] = {
    ids.grouped(100).toSeq.flatMap { idGroup =>
      SQL(s"select * from $TABLE_NAME a, ${InputTag.TABLE_NAME} t where a.$INPUT_ID in ({inputIds}) " +
        s"and a.$TAG_ID = t.${InputTag.ID} order by t.${InputTag.PROPERTY} asc, t.${InputTag.VALUE} asc").
        on("inputIds" -> idGroup).as((InputTag.sqlParser ~ get[SearchInputId](s"$INPUT_ID")).*).
        map { case tag ~ inputId =>
          inputId -> tag
        }
    }.groupBy(_._1).mapValues(_.map(_._2))
  }

  def deleteBySearchInputId(id: SearchInputId)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME where #$INPUT_ID = $id".executeUpdate()
  }

}