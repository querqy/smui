package models

package object SearchManagementModel {

  // TODO evaluate model adjustment for SolrIndex being leading construct and containts a List[SearchInput]
  case class SolrIndex(id: Option[Long] = None,
                       name: String,
                       description: String)

  case class SynonymRule(id: Option[Long] = None,
                         synonymType: Int,
                         term: String,
                         isActive: Boolean)

  case class UpDownRule(id: Option[Long] = None,
                        upDownType: Int,
                        boostMalusValue: Int,
                        term: String,
                        isActive: Boolean)

  case class FilterRule(id: Option[Long] = None,
                        term: String,
                        isActive: Boolean)

  case class DeleteRule(id: Option[Long] = None,
                        term: String,
                        isActive: Boolean)

  // TODO rearrange SearchManagementRepository algorithms purely functional, so that no mutable var-attributes necessary
  case class SearchInput(id: Option[Long] = None,
                         term: String,
                         var synonymRules: List[SynonymRule],
                         var upDownRules: List[UpDownRule],
                         var filterRules: List[FilterRule],
                         var deleteRules: List[DeleteRule])

  // TODO currently not in use ...
  // TODO Consider resolving as Option field in the Rule's itself (e.g. UpDownRule.errorMsgs List[String])
  case class SearchInputValidationResult(inputTermErrorMsg: Option[String],
                                         synonymRulesErrorMsg: List[Map[Long, String]],
                                         upDownRulesErrorMsg: List[Map[Long, String]],
                                         filterRulesErrorMsg: List[Map[Long, String]],
                                         deleteRulesErrorMsg: List[Map[Long, String]])

  case class SuggestedSolrField(id: Option[Long] = None,
                                name: String)

}
