package models.input

import java.io.InputStream
import java.sql.Connection

import models.SolrIndex
import play.api.Logger
import play.api.libs.json.{Json, OFormat}

case class PredefinedTag(property: Option[String],
                         value: String,
                         solrIndexName: Option[String],
                         exported: Option[Boolean]) {

}

object PredefinedTag {

  val logger = Logger(getClass)

  implicit val jsonFormat: OFormat[PredefinedTag] = Json.format[PredefinedTag]

  def fromStream(stream: InputStream): Seq[PredefinedTag] = {
    try {
      Json.parse(stream).as[Seq[PredefinedTag]]
    } finally {
      stream.close()
    }
  }

  def updateInDB(predefinedTags: Seq[PredefinedTag])(implicit connection: Connection): (Seq[InputTagId], Seq[InputTag]) = {
    val indexIdsByName = SolrIndex.listAll.map(i => i.name -> i.id).toMap
    val tagsInDBByContent = InputTag.loadAll().map(t => t.tagContent -> t).toMap

    val newTags = predefinedTags.map { tag =>
      TagContent(tag.solrIndexName.flatMap(indexIdsByName.get), tag.property, tag.value) -> tag
    }.toMap

    val toDelete = tagsInDBByContent.filter { case (content, tag) => tag.predefined && !newTags.contains(content) }.map(_._2.id).toSeq
    val toInsert = newTags.filter(t => !tagsInDBByContent.contains(t._1)).map { case (tc, t) =>
      InputTag.create(tc.solrIndexId, t.property, t.value, t.exported.getOrElse(true), predefined = true)
    }.toSeq

    InputTag.insert(toInsert: _*)
    InputTag.deleteByIds(toDelete)
    if (toDelete.nonEmpty || toInsert.nonEmpty) {
      logger.info(s"Inserted ${toInsert.size} new predefined tags into the DB and deleted ${toDelete.size} no longer existing predefined tags.")
    }

    (toDelete, toInsert)
  }

}