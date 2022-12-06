package models.rules

import anorm._
import models.Id
import models.input.SearchInputId
import play.api.Logging

import java.sql.Connection
import java.time.LocalDateTime

trait Rule {

  def id: Id
  def isActive: Boolean

  def status: Int = if (isActive) 0x01 else 0x00

  import Rule._

  def toNamedParameters(searchInputId: SearchInputId): Seq[NamedParameter] = Seq(
    ID -> id.id,
    STATUS -> status,
    SEARCH_INPUT_ID -> searchInputId,
    LAST_UPDATE -> LocalDateTime.now()
  )

}

trait RuleWithTerm extends Rule {

  def term: String

  override def toNamedParameters(searchInputId: SearchInputId): Seq[NamedParameter] = {
    super.toNamedParameters(searchInputId) ++ Seq[NamedParameter](Rule.TERM -> term)
  }

}

trait CommonRuleFields extends Logging {

  val ID = "id"
  val STATUS = "status"
  val SEARCH_INPUT_ID = "search_input_id"
  val LAST_UPDATE = "last_update"
  val TERM = "term"

}

trait RuleObject[T <: Rule] extends CommonRuleFields {

  val TABLE_NAME: String

  def fieldNames: Seq[String] = Seq(ID, STATUS, SEARCH_INPUT_ID, LAST_UPDATE)

  def orderByField: String

  val sqlParser: RowParser[T]

  def updateForSearchInput(searchInputId: SearchInputId, rules: Seq[T])(implicit connection: Connection) {
    // TODO consider to really determine an update/delete diff to ensure that last_update timestamps only updated for affected rules
    logger.debug("RuleObject:updateForSearchInput")
    SQL"delete from #$TABLE_NAME where #$SEARCH_INPUT_ID = $searchInputId".execute()

    if (rules.nonEmpty) {
      BatchSql(s"insert into $TABLE_NAME (${fieldNames.mkString(", ")}) " +
        s"values (${fieldNames.map(f => s"{$f}").mkString(", ")})",
        rules.head.toNamedParameters(searchInputId),
        rules.tail.map(_.toNamedParameters(searchInputId)): _*
      ).execute()
    }
  }

  def deleteBySearchInput(searchInputId: SearchInputId)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME where #$SEARCH_INPUT_ID = $searchInputId".executeUpdate()
  }

  def loadByInputId(searchInputId: SearchInputId)(implicit connection: Connection): List[T] = {
    logger.debug("RuleObject:loadByInputId")
    SQL"select * from #$TABLE_NAME where #$SEARCH_INPUT_ID = $searchInputId order by #$orderByField".as(sqlParser.*)
  }

}

trait RuleObjectWithTerm[T <: RuleWithTerm] extends RuleObject[T] {

  override def fieldNames: Seq[String] = super.fieldNames :+ TERM
  override def orderByField: String = TERM
}

object Rule extends CommonRuleFields {

  val allRules: Seq[RuleObject[_ <: Rule]] = Seq(DeleteRule, FilterRule, RedirectRule, SynonymRule, UpDownRule)

}