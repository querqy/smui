package services

import models.input.SearchInputId
import org.apache.commons.csv.CSVFormat
import play.api.{Configuration, Logging}

import javax.inject.Inject
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

case class RulesUsage(inputId: SearchInputId,
                      keywords: String,
                      frequency: Int)

class RulesUsageService @Inject()(configuration: Configuration,
                                  readerProvider: ReaderProviderDispatcher) extends Logging {

  private val ConfigKeyRuleUsageStatistics = "smui.rule-usage-statistics.location"

  private val CsvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()

  def getRulesUsageStatistics: Option[Seq[RulesUsage]] = {
    configuration.getOptional[String](ConfigKeyRuleUsageStatistics)
      .map { location =>
        logger.info(s"Loading rule usage statistics from ${location}")
        try {
          val reader = readerProvider.openReader(location)
          try {
            CsvFormat.parse(reader).asScala.map { record =>
              RulesUsage(
                SearchInputId(record.get("SMUI_GUID")),
                record.get("USER_QUERY"),
                record.get("FREQUENCY").toInt)
            }.toSeq
          } finally {
            reader.close()
          }
        } catch {
          case e: Exception =>
            logger.error("Could not load rule usage statistics", e)
            Seq.empty
        }
      }
  }

}
