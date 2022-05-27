package models.`export`

import anorm.SqlParser.get
import anorm.{NamedParameter, RowParser, ~}
import models.`export`.SomethingRow.{id, last_update, myJavascriptObject, value0}
import models.input.SearchInputId
import models.rules.Rule.{ID, LAST_UPDATE, SEARCH_INPUT_ID, STATUS}
import models.rules.{RuleWithTerm, SynonymRule, SynonymRuleId}
import models.{Id, IdObject}
import play.api.libs.json.{JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, Json, Writes}

import java.time.LocalDateTime
import java.util.UUID

case class Something(id: SomethingId = SomethingId(),
                     value0: String,
                     last_update: LocalDateTime)  {
//  override def toNamedParameters(searchInputId: SearchInputId): Seq[NamedParameter] = {
//    super.toNamedParameters(searchInputId) ++ Seq[NamedParameter](
//      .TYPE -> synonymType
//    )
//  }
  def toNamedParameters(id: SomethingId): Seq[NamedParameter] = Seq(
    ID -> id.id,
    value0 -> value0,
    LAST_UPDATE -> LocalDateTime.now()
  )

  implicit val writer: Writes[String] = new Writes[(String)] {
    def writes(t: (String)): JsValue = {
      Json.obj(
        "value0" -> myJavascriptObject.value0,
        "value1" -> myJavascriptObject.value1,
        "value2" -> myJavascriptObject.value2,
        "value3" -> myJavascriptObject.value3)
    }
  }

  def getJsValue(): JsValue = {
    val x : JsObject = JsObject(
      Seq (
        "id" -> JsString(id.toString),
        "value0" -> JsString(value0),
        "last_update" -> JsString(last_update.toString)
      )
    )
    x
  }

}

class SomethingId(id: String) extends Id(id)
object SomethingId extends IdObject[SomethingId](new SomethingId(_))

case class MyJsObject(
                  value0: JsValue,
                  value1: JsValue,
                  value2: JsValue,
                  value3: JsValue
                )

object SomethingRow extends Something(id=SomethingId(), value0="", last_update=LocalDateTime.now()) {

  val TABLE_NAME = "something"
  val TYPE = "something"

  val sqlParser: RowParser[Something] = {
    get[SomethingId] (s"$TABLE_NAME.id") ~
      get[String](s"$TABLE_NAME.value0") ~
      get[LocalDateTime](s"$TABLE_NAME.last_update") map { case id ~ value0 ~ last_update =>
      Something(id, value0, last_update)
    }
  }

  val myJavascriptObject: MyJsObject =
    MyJsObject(
      value0 = new JsString("id = " + id),
      value1 = JsObject(Seq("simpleObject" -> JsString("simpleStringValue"))),
      value2 = JsObject(
        Seq(
          "aString" -> JsString("simpleString"),
          "aNumber"  -> JsNumber(4.3),
          "aNull" -> JsNull
        )
      ),
      value3 = JsArray(
        IndexedSeq(
          new JsString("first value of array"),
          new JsString("second value of array"))
      )
    )

//  def getAsJsonRow(): JsObject = {
//    value1 = JsObject(Seq("simpleObject" -> JsString("simpleStringValue"))),
//      value2 = JsObject(
//        Seq(
//          "aString" -> JsString("simpleString"),
//          "aNumber"  -> JsNumber(4.3),
//          "aNull" -> JsNull
//        )
//      ),
//      value3 = JsArray(
//        IndexedSeq(
//          new JsString("first value of array"),
//          new JsString("second value of array"))
//      )
//    )
//  }


}
