package models.validatedimport

import play.api.Logging
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

import java.time.{LocalDateTime, LocalTime}
import java.util
import java.util.UUID
import scala.collection.mutable
import anorm._

case class ValidatedImportData(filename: String, content: String) extends Logging {

  var validTableNames: List[String] = List(
    "alternative_spelling",
    "canonical_spelling",
    "synonym_rule",
    "up_down_rule",
    "redirect_rule",
    "search_input",
    "solr_index",
    "delete_rule",
    "input_tag",
    "tag_2_input",
    "suggested_solr_field",
    "filter_rule")

  var inputJsonValue: Option[JsValue] = None
  var tableCount: Int = 0
  var tableName: String = ""
  var columnList: String = ""
  var currentColumns: IndexedSeq[String] = IndexedSeq()
  var statement: String = ""
  var statements: IndexedSeq[String] = IndexedSeq()
  var solr_index_id: String = ""
  var old_solr_index_id: String = ""
  var a_different_existing_solr_index_id: String = UUID.randomUUID().toString
  var a_shorthand_id: String = ""
  val SUCCESS: Int = 0
  var inputFilename: String = ""

  var allStatementsConcatenated: String = ""
  var replacementIds: mutable.HashMap[String, String] = mutable.HashMap()

  def isValid: Boolean = {
    parseJson == SUCCESS
  }

  def parseJson: Int = {
    this.inputFilename = filename
    a_different_existing_solr_index_id =  UUID.randomUUID().toString
    a_shorthand_id = a_different_existing_solr_index_id.substring(0, 8)
    logger.debug("ValidatedImportData.parseJson():1 begin parsing")
    inputJsonValue = Option(Json.parse(content))
    if (inputJsonValue.isDefined) {
        val tables: IndexedSeq[JsValue] = inputJsonValue.get.as[JsArray].value
        processTables(tables)
    }
    logger.debug("ValidatedImportData.parseJson():3 end parsing")
    SUCCESS
  }

  def processTables(input: IndexedSeq[JsValue]) : Unit = {
    if (input.headOption.isEmpty) {
      //printStatements(statements)
      concatenateStatements(statements)
      logger.debug(allStatementsConcatenated)
      ()
    } else {
      this.tableName = input.head.\("tableName").as[String]
      if (validTableNames.contains(tableName)) {
        this.tableCount = this.tableCount + 1

        this.statement = "INSERT INTO " + tableName + " "

        val columns: IndexedSeq[JsValue] = input.head.\("columns").as[JsArray].value
        this.currentColumns = IndexedSeq()
        processColumns(columns, 0, (columns.size - 1))
        //logger.debug(currentColumns(1))

        val rows: IndexedSeq[JsValue] = input.head.\("rows").as[JsArray].value
        processRows(rows, 0, (rows.size - 1))
        this.statement += ";"
        statements = statements :+ statement
        //logger.debug(statement)
        //logger.debug("tableCount: " + tableCount)

        processTables(input.drop(1))
      }
      else {
        logger.debug("Unrecognized tableName: " + tableName)
        //skip any tables that we don't recognize
        processTables(input.drop(1))
      }
    }
  }

  def processColumns(input: IndexedSeq[JsValue], index: Int, lastIndex: Int): Unit = {
    if (input.headOption.isEmpty) {
      this.statement += ") VALUES "
      ()
    } else {
      if (index == 0) this.statement += "("
      var columnName = input.head.toString().replace("\"", "")
      this.currentColumns = this.currentColumns :+ columnName
      this.statement += columnName
      if (index != lastIndex) this.statement += ","
      processColumns(input.drop(1), (index + 1), lastIndex)
    }
  }

  def processRow(input: IndexedSeq[JsValue], index: Int, lastIndex: Int): Unit = {
    if (input.headOption.isEmpty) {
      this.statement += ")"
      ()
    } else {
      if (index == 0) this.statement += "("
      var rawCellValue = input.head.toString().replace("\"", "")
      var cellValue = input.head.toString().replace("\"", "\'")

      if (this.tableName.equals("solr_index")) {
        //logger.debug(currentColumns(index))
        if (currentColumns(index).equals("id")) {
          this.old_solr_index_id = rawCellValue;
          this.solr_index_id = "'" + this.a_different_existing_solr_index_id + "'"
          cellValue = solr_index_id
        } else if (currentColumns(index).equals("name") || currentColumns(index).equals("description")) {
          var old_id_shorthand = this.old_solr_index_id.substring(0, 8)
          var sds = shortDistinguishedString
          logger.debug("cur col:" + currentColumns(index))
          logger.debug("rawCellValue: " + rawCellValue)
          logger.debug("old_id_shorthand: " + old_id_shorthand)
          //this.solr_index_id = "'" + this.a_different_existing_solr_index_id + "'"
          cellValue = "'" + rawCellValue + ", (file: " + inputFilename + " copied from key: " + old_id_shorthand + ") " + sds + "'"
        }
      }
      else if (currentColumns(index).equals("id") || currentColumns(index).equals("search_input_id") || currentColumns(index).equals("input_id")) {
        if (!replacementIds.contains(cellValue)) {
          replacementIds.put(cellValue, UUID.randomUUID().toString)
        }
        //logger.debug("replacing " + cellValue)
        cellValue = "'" + replacementIds.get(cellValue).head + "'"
        //logger.debug("replaced it with :" + cellValue)
      }

      if (currentColumns(index).equals("last_update")) {
        //ISO_LOCAL_DATE + " " + ISO_LOCAL_TIME
        val sqlDate = java.sql.Date.valueOf(LocalDateTime.now.toLocalDate)
        val sqlTime = java.sql.Time.valueOf(LocalTime.now)
        var isoLocalDateTimeStr = sqlDate + " " + sqlTime
        cellValue = "'" + isoLocalDateTimeStr + "'"
      }

      if (this.tableName.equals("search_input") ||
        this.tableName.equals("suggested_solr_field") ||
        this.tableName.equals("input_tag")
      ) {
        if (currentColumns(index).equals("solr_index_id")) {
          cellValue = "'" + this.a_different_existing_solr_index_id + "'"
        }
      }

      this.statement += cellValue
      if (index != lastIndex) this.statement += ","

      processRow(input.drop(1), (index + 1), lastIndex)
    }
  }

  def processRows(input: IndexedSeq[JsValue], index: Int, lastIndex: Int): Unit = {
    if (input.headOption.isEmpty) {
      ()
    } else {
      val row: IndexedSeq[JsValue] = input.head.as[JsArray].value
      processRow(row, 0, (row.size - 1))
      if (index != lastIndex) {
        this.statement += ","
      }
      processRows(input.drop(1), (index + 1), lastIndex)
    }
  }

  def printStatements(input: IndexedSeq[String]): Unit = {
    if (input.headOption.isEmpty) {
      ()
    } else {
      logger.debug(input.head)
      this.allStatementsConcatenated = this.allStatementsConcatenated + input.head
      printStatements(input.drop(1))
    }
  }

  def concatenateStatements(input: IndexedSeq[String]): Unit = {
    if (input.headOption.isEmpty) {
      ()
    } else {
      this.allStatementsConcatenated = this.allStatementsConcatenated + " " + input.head
      printStatements(input.drop(1))
    }
  }

  def shortDistinguishedString: String = {
    UUID.randomUUID().toString.substring(0, 8)
  }

}