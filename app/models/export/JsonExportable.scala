package models.`export`

import play.api.libs.json.{JsString, JsValue}

trait JsonExportable {

  def getTableName: JsString

  def getRow: JsValue

  def getColumns: JsValue

}
