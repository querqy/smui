package models.input

import models.SolrIndexId

/**
  * Version of a tag with only the basic information.
  */
case class TagContent(solrIndexId: Option[SolrIndexId],
                      property: Option[String],
                      value: String) {

}
