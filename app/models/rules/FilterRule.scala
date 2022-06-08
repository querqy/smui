package models.rules

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.input.SearchInputId
import models.rules.DeleteRule.{LAST_UPDATE, SEARCH_INPUT_ID, TABLE_NAME}
import models.{Id, IdObject, Status}
import play.api.libs.json.{JsArray, JsNumber, JsString, JsValue, Json, OFormat}

import java.time.LocalDateTime

class FilterRuleId(id: String) extends Id(id)
object FilterRuleId extends IdObject[FilterRuleId](new FilterRuleId(_))


case class FilterRule(id: FilterRuleId = FilterRuleId(),
                      term: String,
                      isActive: Boolean) extends RuleWithTerm {

}

object FilterRule extends RuleObjectWithTerm[FilterRule] {

  val TABLE_NAME = "filter_rule"

  implicit val jsonFormat: OFormat[FilterRule] = Json.format[FilterRule]

  override val sqlParser: RowParser[FilterRule] = {
    get[FilterRuleId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") map { case id ~ term ~ status =>
      FilterRule(id, term, Status.isActiveFromStatus(status))
    }
  }

}