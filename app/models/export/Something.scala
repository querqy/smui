package models.`export`

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.`export`.Something.myJavascriptObject
import models.{Id, IdObject}
import play.api.libs.json.{JsArray, JsString, JsValue, Json, Writes}

import java.time.LocalDateTime
import java.util.UUID

class SomethingId(id: String) extends Id(id)
object SomethingId extends IdObject[SomethingId](new SomethingId(_))

case class Something(id: SomethingId = SomethingId(),
                     value_0: String,
                     last_update: LocalDateTime) extends JsonExportable {

  implicit val writer: Writes[String] = new Writes[(String)] {
    def writes(t: (String)): JsValue = {
      Json.obj(
        "id" -> myJavascriptObject.id,
        "value_0" -> myJavascriptObject.value_0,
        "last_update" -> myJavascriptObject.last_update)
    }
  }

  def getTableName: JsString = JsString("something")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("value_0"),
        JsString("last_update")
      )
    )
  }

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(value_0),
        JsString(last_update.toString)
      )
    )
  }

}

case class MyJsObject(
                  id: JsValue,
                  value_0: JsValue,
                  last_update: JsValue
                )

object Something {

  val TABLE_NAME = "something"
  val ID = "id"
  val VALUE_0 = "value_0"
  val LAST_UPDATE = "last_update"

  val sqlParser: RowParser[Something] = {
    get[SomethingId] (s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$VALUE_0") ~
      get[LocalDateTime](s"$TABLE_NAME.last_update") map { case id ~ value_0 ~ last_update =>
      Something(id, value_0, last_update)
    }
  }

  val selectAllStatement : String = {
    s"select $TABLE_NAME.$ID, " +
      s"$TABLE_NAME.$VALUE_0, " +
      s"$TABLE_NAME.$LAST_UPDATE from $TABLE_NAME"
  }

  val myJavascriptObject: MyJsObject = {
    MyJsObject(
      id = JsString(UUID.randomUUID().toString),
      value_0 = JsString("this is some value for value_0"),
      last_update = JsString(LocalDateTime.now().toString)
//      value1 = JsObject(Seq("simpleObject" -> JsString("simpleStringValue"))),
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
    )
  }

}
