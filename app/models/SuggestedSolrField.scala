package models

import play.api.libs.json.{Json, OFormat}

class SuggestedSolrFieldId(id: String) extends Id(id)
object SuggestedSolrFieldId extends IdObject[SuggestedSolrFieldId](new SuggestedSolrFieldId(_))


case class SuggestedSolrField(id: SuggestedSolrFieldId = SuggestedSolrFieldId(),
                         name: String) {

}

object SuggestedSolrField {

  implicit val jsonFormat: OFormat[SuggestedSolrField] = Json.format[SuggestedSolrField]

}