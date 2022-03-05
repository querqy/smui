package models.rules

import anorm.SqlParser.get
import anorm.{NamedParameter, RowParser, ~}
import models.input.SearchInputId
import models.{Id, IdObject, Status}
import play.api.libs.json.{Json, OFormat}

class RedirectRuleId(id: String) extends Id(id)
object RedirectRuleId extends IdObject[RedirectRuleId](new RedirectRuleId(_))


case class RedirectRule(id: RedirectRuleId = RedirectRuleId(),
                        target: String,
                        isActive: Boolean) extends Rule {

  override def toNamedParameters(searchInputId: SearchInputId): Seq[NamedParameter] = {
    super.toNamedParameters(searchInputId) ++ Seq[NamedParameter](
      RedirectRule.TARGET -> target
    )
  }
}

object RedirectRule extends RuleObject[RedirectRule] {

  val TABLE_NAME = "redirect_rule"
  val TARGET = "target"

  implicit val jsonFormat: OFormat[RedirectRule] = Json.format[RedirectRule]

  override def fieldNames: Seq[String] = super.fieldNames :+ TARGET

  override def orderByField: String = TARGET

  override val sqlParser: RowParser[RedirectRule] = {
    get[RedirectRuleId](s"$ID") ~
      get[String](s"$TARGET") ~
      get[Int](s"$STATUS") map { case id ~ target ~ status =>
      RedirectRule(id, target, Status.isActiveFromStatus(status))
    }
  }

}