package models.`export`

import play.api.libs.json.{JsArray, JsNull, JsNumber, JsObject, JsString, JsValue, Json, Writes}

//case class MyJsObject(
//                       somethings: JsValue,
//                     )

class Somethings() {

//  val myJavascriptObject: MyJsObject =
//    MyJsObject(
//      somethings = JsArray(
//        IndexedSeq(
//          new JsString("first value of array"),
//          new JsString("second value of array"))
//      )
//    )

//  implicit val writer = new Writes[(String)] {
//    def writes(t: (String)): JsValue = {
//      Json.obj(
//        "somethings" -> myJavascriptObject.somethings
//      )
//    }
//  }

  def asIndexedSeqForJs(indexedSeqSource : IndexedSeq[Something]): JsValue = {
    val exporter : Exporter = new Exporter()
    //var indexedSeqSource : IndexedSeq[Something] = exporter.getAllSomethings()
    var target1: List[String] = List[String]()
    println("#In Somethings.asIndexedSeqForJs : size of target1: " + target1.size)
    target1 = target1 :+ "hi"
    println("->In Somethings.asIndexedSeqForJs : size of target1: " + target1.size)

    var target: IndexedSeq[JsValue] = IndexedSeq[JsValue]()
    target :+ JsString("hi")
    println("->In Somethings.asIndexedSeqForJs : size of target: " + target.size)
    for((element,index) <- indexedSeqSource.view.zipWithIndex) {
      println("In Somethings.asIndexedSeqForJs : String #" + index + " is " + element.getJsValue()) //CJM DELETE
      target = target :+ element.getJsValue()
    }
    println("%In Somethings.asIndexedSeqForJs : size of target: " + target.size)
    val x : JsValue = JsObject(Seq("somethings-asIndexedSeqForJs-was-here" -> JsArray(target.toIndexedSeq)))
    x
  }

}
