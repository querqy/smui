package models.`export`

import play.api.libs.json.{JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, Json, Writes}

case class MyJsObject(
                  value0: JsValue,
                  value1: JsValue,
                  value2: JsValue,
                  value3: JsValue
                )

class Something(solrIndexId: String) {

  val myJavascriptObject: MyJsObject =
    MyJsObject(
      value0 = new JsString("solrIndexId = " + solrIndexId),
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

  implicit val writer = new Writes[(String)] {
    def writes(t: (String)): JsValue = {
      Json.obj(
        "value0" -> myJavascriptObject.value0,
        "value1" -> myJavascriptObject.value1,
        "value2" -> myJavascriptObject.value2,
        "value3" -> myJavascriptObject.value3)
    }
  }

}
