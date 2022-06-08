package models.rules

import anorm.SqlParser.get
import anorm._
import models.input.SearchInputId
import models.{Id, IdObject, Status}
import play.api.libs.json.{Json, OFormat}

import java.sql.Connection

class SynonymRuleId(id: String) extends Id(id)
object SynonymRuleId extends IdObject[SynonymRuleId](new SynonymRuleId(_))

case class SynonymRule(id: SynonymRuleId = SynonymRuleId(),
                       synonymType: Int,
                       term: String,
                       isActive: Boolean) extends RuleWithTerm {

  override def toNamedParameters(searchInputId: SearchInputId): Seq[NamedParameter] = {
    super.toNamedParameters(searchInputId) ++ Seq[NamedParameter](
      SynonymRule.TYPE -> synonymType
    )
  }

}

object SynonymRule extends RuleObjectWithTerm[SynonymRule] {

  val TABLE_NAME = "synonym_rule"
  val TYPE = "synonym_type"

  val TYPE_UNDIRECTED = 0
  val TYPE_DIRECTED = 1

  implicit val jsonFormat: OFormat[SynonymRule] = Json.format[SynonymRule]

  override def fieldNames: Seq[String] = super.fieldNames :+ TYPE

  val sqlParser: RowParser[SynonymRule] = {
    get[SynonymRuleId](s"$TABLE_NAME.$ID") ~
      get[Int](s"$TABLE_NAME.$TYPE") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") map { case id ~ synonymType ~ term ~ status =>
        SynonymRule(id, synonymType, term, Status.isActiveFromStatus(status))
    }
  }

  def loadUndirectedBySearchInputIds(ids: Seq[SearchInputId])(implicit connection: Connection): Map[SearchInputId, Seq[SynonymRule]] = {
    ids.grouped(100).toSeq.flatMap { idGroup =>
      SQL"select * from #$TABLE_NAME where #$TYPE = #$TYPE_UNDIRECTED AND #$SEARCH_INPUT_ID in ($idGroup)".as((sqlParser ~ get[SearchInputId](SEARCH_INPUT_ID)).*).map { case rule ~ id =>
        id -> rule
      }
    }.groupBy(_._1).mapValues(_.map(_._2))
  }

}