package models.config

import play.api.{Configuration, Logging, ConfigLoader}
import play.api.libs.json._

import javax.inject.Inject

import scala.util.Try

package object TargetEnvironment extends Logging {

    val SUPER_DEFAULT_INPUT = s"""{
}"""

    case class TargetEnvironmentDescription(
        rulesCollection: String,
        tenantTag: Option[String],
        previewUrlTemplate: String
    )

    case class TargetEnvironmentGroup(
        id: String,
        targetEnvironments: Seq[TargetEnvironmentDescription]
    )

    case class TargetEnvironmentInstance(
        id: String,
        targetEnvironmentGroups: Seq[TargetEnvironmentGroup]
    )

    type TargetEnvironmentConfig = Seq[TargetEnvironmentInstance]
    val SUPER_DEFAULT_OUTPUT = Seq.empty

    @javax.inject.Singleton
    class TargetEnvironmentConfigService @Inject()(appConfig: Configuration) {

        implicit val jsonFormatTargetEnvironmentDescription: OFormat[TargetEnvironmentDescription] = Json.format[TargetEnvironmentDescription]

        implicit val jsonFormatTargetEnvironmentConfig = new Format[ Seq[TargetEnvironmentInstance] ] {
            
            def writes(targetEnvironmentConfig: Seq[TargetEnvironmentInstance]): JsValue =
                ??? // TODO implement, if necessary
                /*
                JsArray(
                    targetEnvironmentConfig.map( envInst =>
                        Json.obj( envInst.id -> "TBD" )
                    )
                )
                */
            
            private def parseTargetEnvironmentGroups(jo: JsObject) = {
                jo.fieldSet.map({inner =>
                    val targetEnvironments = inner._2.as[JsArray].value.map(innerEntry => {
                        // TODO implicit JSON Read from above can not be applied. Need to read the config manually.
                        //innerEntry.as[TargetEnvironmentDescription]

                        val previewUrlTemplate = (innerEntry \ "previewUrlTemplate").as[String]

                        if( !previewUrlTemplate.contains("$QUERY") ) {
                            throw new Exception("previewUrlTemplate does not contain $QUERY placeholder")
                        }

                        TargetEnvironmentDescription(
                            rulesCollection = (innerEntry \ "rulesCollection").as[String],
                            tenantTag = (innerEntry \ "tenantTag").asOpt[String],
                            previewUrlTemplate = previewUrlTemplate
                        )
                    }).toSeq
                    TargetEnvironmentGroup(
                        id = inner._1,
                        targetEnvironments = targetEnvironments
                    )
                }).toSeq
            }

            def reads(jv: JsValue): JsResult[ Seq[TargetEnvironmentInstance] ] = {
                val jo = jv.asInstanceOf[JsObject]
                JsSuccess(
                    jo.fieldSet.map(inner =>
                        TargetEnvironmentInstance(
                            id = inner._1,
                            targetEnvironmentGroups = parseTargetEnvironmentGroups(inner._2.asInstanceOf[JsObject])
                        )
                    ).toSeq
                )
            }
        }

        def read: TargetEnvironmentConfig = {
            val strJsonTargetEnvConf = appConfig
                .getOptional[String]("smui.target-environment.config")
                .getOrElse({
                    logger.warn(s"In TargetEnvironmentConfigService :: read :: target environment configuration could not be read. There is no target environment (for e.g. preview links) configured.")
                    SUPER_DEFAULT_INPUT
                })
            
            try {
                Json.parse(strJsonTargetEnvConf).validate[ Seq[TargetEnvironmentInstance] ].asOpt match {
                    case None => {
                        logger.error(s"In TargetEnvironmentConfigService :: read :: target environment configuration could not be parsed from JSON. There is no target environment (for e.g. preview links) configured.")
                        SUPER_DEFAULT_OUTPUT
                    }
                    case Some(customTargetEnvironmentConfig: Seq[TargetEnvironmentInstance]) => {
                        customTargetEnvironmentConfig
                    }
                }
            }
            catch {
                case e: Exception => {
                    logger.error(s"In TargetEnvironmentConfigService :: read :: target environment configuration could not be parsed from JSON with error = ${e}. There is no target environment (for e.g. preview links) configured.")
                    SUPER_DEFAULT_OUTPUT
                }
            }
        }
    }

}

