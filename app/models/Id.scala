package models

import java.util.UUID

import play.api.libs.json.{JsString, Reads, Writes}
import java.sql.PreparedStatement

import anorm.{Column, NotNullGuard, ParameterMetaData, ToStatement}

abstract class Id(val id: String) {

  override def toString: String = id

  def canEqual(other: Any): Boolean = other.isInstanceOf[Id]

  override def equals(other: Any): Boolean = other match {
    case that: Id =>
      (that canEqual this) &&
        id == that.id
    case _ => false
  }

  override def hashCode(): Int = id.hashCode
}

object Id {

  implicit val jsonWrites: Writes[Id] = Writes[Id](id => JsString(id.id))

}

abstract class IdObject[T <: Id](fromString: String => T) {

  def apply(str: String): T = fromString(str)

  implicit val jsonWrites: Writes[T] = Id.jsonWrites
  implicit val jsonReads: Reads[T] = Reads[T](_.validate[String].map(apply))

  def apply(): T = apply(UUID.randomUUID().toString)

  implicit def customToStatement: ToStatement[T] = new ToStatement[T] with NotNullGuard {
    def set(statement: PreparedStatement, i: Int, value: T): Unit = {
      statement.setString(i, value.id)
    }
  }

  implicit def customParamMeta: ParameterMetaData[T] = new ParameterMetaData[T] {
    override val sqlType = "VARCHAR"
    override def jdbcType: Int = java.sql.Types.VARCHAR
  }

  implicit val columnToId: Column[T] = {
    Column.columnToString.map(fromString)
  }

}
