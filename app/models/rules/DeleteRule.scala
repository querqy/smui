package models.rules

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.input.SearchInputId
import models.{Id, IdObject, Status}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

class DeleteRuleId(id: String) extends Id(id)
object DeleteRuleId extends IdObject[DeleteRuleId](new DeleteRuleId(_))


case class DeleteRule(id: DeleteRuleId = DeleteRuleId(),
                      term: String,
                      isActive: Boolean) extends RuleWithTerm {

}

object DeleteRule extends RuleObjectWithTerm[DeleteRule] {

  val TABLE_NAME = "delete_rule"

  implicit val jsonFormat: OFormat[DeleteRule] = Json.format[DeleteRule]

  override val sqlParser: RowParser[DeleteRule] = {
    get[DeleteRuleId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") map { case id ~ term ~ status =>
      DeleteRule(id, term, Status.isActiveFromStatus(status))
    }
  }

}