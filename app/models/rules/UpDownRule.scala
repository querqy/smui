package models.rules

import anorm.SqlParser.get
import anorm.{NamedParameter, RowParser, ~}
import models.input.SearchInputId
import models.{Id, IdObject, Status}
import play.api.libs.json.{Json, OFormat}

class UpDownRuleId(id: String) extends Id(id)
object UpDownRuleId extends IdObject[UpDownRuleId](new UpDownRuleId(_))


case class UpDownRule(id: UpDownRuleId = UpDownRuleId(),
                      upDownType: Int,
                      boostMalusValue: Int,
                      term: String,
                      isActive: Boolean) extends RuleWithTerm {

  override def toNamedParameters(searchInputId: SearchInputId): Seq[NamedParameter] = {
    super.toNamedParameters(searchInputId) ++ Seq[NamedParameter](
      UpDownRule.BOOST_MALUS_VALUE -> boostMalusValue,
      UpDownRule.UP_DOWN_TYPE -> upDownType
    )
  }

}

object UpDownRule extends RuleObjectWithTerm[UpDownRule] {

  val TABLE_NAME = "up_down_rule"

  val UP_DOWN_TYPE = "up_down_type"
  val BOOST_MALUS_VALUE = "boost_malus_value"

  val TYPE_UP = 0
  val TYPE_DOWN = 1

  override def fieldNames: Seq[String] = super.fieldNames ++ Seq(BOOST_MALUS_VALUE, UP_DOWN_TYPE)

  implicit val jsonFormat: OFormat[UpDownRule] = Json.format[UpDownRule]

  val sqlParser: RowParser[UpDownRule] = {
    get[UpDownRuleId](s"$TABLE_NAME.$ID") ~
      get[Int](s"$TABLE_NAME.$UP_DOWN_TYPE") ~
      get[Int](s"$TABLE_NAME.$BOOST_MALUS_VALUE") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") map { case id ~ upDownType ~ boostMalusValue ~ term ~ status =>
      UpDownRule(id, upDownType, boostMalusValue, term, Status.isActiveFromStatus(status))
    }
  }

}