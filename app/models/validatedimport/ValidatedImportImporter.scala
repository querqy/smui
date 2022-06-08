package models.validatedimport

import anorm.SQL
import models.DatabaseExecutionContext
import models.FeatureToggleModel.FeatureToggleService
import play.api.{Logging, db}
import play.api.db.DBApi

import javax.inject.Inject

@javax.inject.Singleton
class ValidatedImportImporter @Inject()(validatedImportData: ValidatedImportData,
                                        dbapi: DBApi,
                                        toggleService: FeatureToggleService)
                                       (implicit ec: DatabaseExecutionContext) extends Logging {

  private val db = dbapi.database("default")

  def performImport(): Unit = db.withTransaction {
    implicit connection => {
      if (validatedImportData.isValid) {
        logger.debug(validatedImportData.allStatementsConcatenated)
        validatedImportData.statements.foreach(statement => SQL(statement).execute())
      }
      else {
        logger.debug("ValidatedImportImporter.performImport():2: oops bad data?")
      }
    }
  }

}
