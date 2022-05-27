package models.`export`

import play.api.libs.json.JsValue
import play.api.Logging

import java.time.LocalDateTime

class Exporter extends Logging {

  var somethings1: IndexedSeq[Something] = IndexedSeq(
    new Something(SomethingId(), "something1", LocalDateTime.now()),
    new Something(SomethingId(), "something2", LocalDateTime.now())
  )

  def getAllSomethingsForJs(): JsValue = {
    val source : IndexedSeq[Something] = this.somethings1
    val converter : Somethings = new Somethings()
    val x : JsValue = converter.asIndexedSeqForJs(source)
    x
  }

  def getAllSomethingsForJs(somethingsInput : IndexedSeq[Something]): JsValue = {
    logger.debug("In Exporter:getAllSomethingsForJs():1")
    val converter : Somethings = new Somethings()
    val x : JsValue = converter.asIndexedSeqForJs(somethingsInput)
    x
  }

}
