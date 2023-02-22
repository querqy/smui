package models.config

import org.scalatest.{FlatSpec, Matchers}

import play.api.inject.Injector
import play.api.{Application, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.db.{Database, Databases}

import com.typesafe.config._

import models.config.TargetEnvironment._

class TargetEnvironmentConfigSpec extends FlatSpec with Matchers {

  private val db: Database = Databases.inMemory()

  private val BASE_APP_CONFIG: Seq[(String, Any)] = Seq(
    "db.default.url" -> db.url,
    "db.default.driver" -> "org.h2.Driver",
    "db.default.username" -> "",
    "db.default.password" -> ""
  )

  def applicationWithAppConfig(additionalAppConfig: Seq[(String, Any)]) = {

    val allAppConfig: Seq[(String, Any)] =
      BASE_APP_CONFIG ++
        additionalAppConfig

    new GuiceApplicationBuilder().
      in(Mode.Test).
      configure(allAppConfig: _*).
      build()
  }

  def shutdownAppAndDb(application: Application) = {
    application.stop()
    db.shutdown()
  }

  def createAppAndGetConf(customTargetEnvConf: Option[String], preliveActive: Boolean = false): (Application, TargetEnvironmentConfig) = {
    
    val applicationConf = (customTargetEnvConf match {
      case None =>
        Seq()        
      case Some(targetEnvConf) =>
        Seq(
          "smui.target-environment.config" -> targetEnvConf
        )
    // returns Seq(of config parameters)
    }) ++ (if( preliveActive )
      Seq(
        "toggle.rule-deployment.pre-live.present" -> "true"
      )
    else
      Seq())

    val application = applicationWithAppConfig(applicationConf)

    val injector: Injector = application.injector
    val targetEnvironmentConfigService = injector.instanceOf[TargetEnvironmentConfigService]

    val retTargetEnvConf = targetEnvironmentConfigService.read

    (application, retTargetEnvConf)
  }

  val DEFAULT_TARGET_ENV_CONF_MODEL = Seq(
    TargetEnvironmentInstance(
      id = "LIVE",
      targetEnvironmentGroups = Seq(
        TargetEnvironmentGroup(
          id = "en",
          targetEnvironments = Seq(
            TargetEnvironmentDescription(
              rulesCollection = "AmazonEN",
              tenantTag = None,
              previewUrlTemplate = "https://www.amazon.com/s?k=$QUERY"
            )
          )
        )
      )
    )
  )

  "Default target environment config (JSON)" should "be evaluated into the correct SMUI config model" in {

    val (application, retTargetEnvConf) = createAppAndGetConf(None)

    retTargetEnvConf shouldEqual DEFAULT_TARGET_ENV_CONF_MODEL

    shutdownAppAndDb(application)
  }

  "Maximum custom target environment config for LIVE and PRELIVE (JSON)" should "also be evaluated correctly" in {

    val VALID_CUSTOM_TARGET_ENV_CONF = s"""{
|    "LIVE": {
|      "de": [
|        {
|          "rulesCollection": "MainTenantDE",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/de/main-tenant/search?query=$$QUERY"
|        }, {
|          "rulesCollection": "AlternativeTenantDE",
|          "tenantTag": "tenant:ALTERNATIVE",
|          "previewUrlTemplate": "https://www.example.com/de/alternative-tenant/search?query=$$QUERY"
|        }
|      ],
|      "fr": [
|        {
|          "rulesCollection": "MainTenantFR",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/fr/main-tenant/search?query=$$QUERY"
|        }, {
|          "rulesCollection": "AlternativeTenantFR",
|          "tenantTag": "tenant:ALTERNATIVE",
|          "previewUrlTemplate": "https://www.example.com/fr/alternative-tenant/search?query=$$QUERY"
|        }
|      ]
|    },
|    "PRELIVE": {
|      "de": [
|        {
|          "rulesCollection": "MainTenantDE",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/staging/de/main-tenant/search?query=$$QUERY"
|        }, {
|          "rulesCollection": "AlternativeTenantDE",
|          "tenantTag": "tenant:ALTERNATIVE",
|          "previewUrlTemplate": "https://www.example.com/staging/de/alternative-tenant/search?query=$$QUERY"
|        }
|      ],
|      "fr": [
|        {
|          "rulesCollection": "MainTenantFR",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/staging/fr/main-tenant/search?query=$$QUERY"
|        }, {
|          "rulesCollection": "AlternativeTenantFR",
|          "tenantTag": "tenant:ALTERNATIVE",
|          "previewUrlTemplate": "https://www.example.com/staging/fr/alternative-tenant/search?query=$$QUERY"
|        }
|      ]
|    }
|}""".stripMargin

    val (application, retTargetEnvConf) = createAppAndGetConf(
      Some(VALID_CUSTOM_TARGET_ENV_CONF),
      true
    )

    retTargetEnvConf shouldBe Seq(
      TargetEnvironmentInstance(
        "LIVE",
        Seq(
          TargetEnvironmentGroup(
            "de",
            Seq(
              TargetEnvironmentDescription(
                "MainTenantDE",
                None,
                "https://www.example.com/de/main-tenant/search?query=$QUERY"
              ),
              TargetEnvironmentDescription(
                "AlternativeTenantDE",
                Some("tenant:ALTERNATIVE"),
                "https://www.example.com/de/alternative-tenant/search?query=$QUERY"
              )
            )
          ),
          TargetEnvironmentGroup(
            "fr",
            Seq(
              TargetEnvironmentDescription(
                "MainTenantFR",
                None,
                "https://www.example.com/fr/main-tenant/search?query=$QUERY"
              ),
              TargetEnvironmentDescription(
                "AlternativeTenantFR",
                Some("tenant:ALTERNATIVE"),
                "https://www.example.com/fr/alternative-tenant/search?query=$QUERY"
              )
            )
          )
        )
      ),
      TargetEnvironmentInstance(
        "PRELIVE",
        Seq(
          TargetEnvironmentGroup(
            "de",
            Seq(
              TargetEnvironmentDescription(
                "MainTenantDE",
                None,
                "https://www.example.com/staging/de/main-tenant/search?query=$QUERY"
              ),
              TargetEnvironmentDescription(
                "AlternativeTenantDE",
                Some("tenant:ALTERNATIVE"),
                "https://www.example.com/staging/de/alternative-tenant/search?query=$QUERY"
              )
            )
          ),
          TargetEnvironmentGroup(
            "fr",
            Seq(
              TargetEnvironmentDescription(
                "MainTenantFR",
                None,
                "https://www.example.com/staging/fr/main-tenant/search?query=$QUERY"
              ),
              TargetEnvironmentDescription(
                "AlternativeTenantFR",
                Some("tenant:ALTERNATIVE"),
                "https://www.example.com/staging/fr/alternative-tenant/search?query=$QUERY"
              )
            )
          )
        )
      )
    )

    shutdownAppAndDb(application)
  }

  "An empty SMUI config model for invalid input" should "result from missing language structure level in the config JSON" in {

    val INVALID_CUSTOM_TARGET_ENV_CONF = s"""{
|    "LIVE": [
|        {
|          "rulesCollection": "MainTenantDE",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/de/main-tenant/search?query=$$QUERY"
|        }, {
|          "rulesCollection": "AlternativeTenantDE",
|          "tenantTag": "tenant:ALTERNATIVE",
|          "previewUrlTemplate": "https://www.example.com/de/alternative-tenant/search?query=$$QUERY"
|        }
|      ]
|}""".stripMargin

    val (application, retTargetEnvConf) = createAppAndGetConf(Some(INVALID_CUSTOM_TARGET_ENV_CONF))

    print("retTargetEnvConf = <<<" + retTargetEnvConf + ">>>")

    retTargetEnvConf shouldBe List.empty
    // TODO "class play.api.libs.json.JsArray cannot be cast to class play.api.libs.json.JsObject"

    shutdownAppAndDb(application)

  }

  it should "result from a missing reference to a rules collection" in {

    val INVALID_CUSTOM_TARGET_ENV_CONF = s"""{
|    "LIVE": {
|      "de": [
|        {
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/de/main-tenant/search?query=$$QUERY"
|        }
|      ]
|    }
|}""".stripMargin

    val (application, retTargetEnvConf) = createAppAndGetConf(Some(INVALID_CUSTOM_TARGET_ENV_CONF))

    print("retTargetEnvConf = <<<" + retTargetEnvConf + ">>>")

    retTargetEnvConf shouldBe List.empty
    // TODO "JsonValidationError(List('rulesCollection' is undefined on object"

    shutdownAppAndDb(application)

  }

  it should "result from a missing preview URL template string" in {

    val INVALID_CUSTOM_TARGET_ENV_CONF = s"""{
|    "LIVE": {
|      "de": [
|        {
|          "rulesCollection": "MainTenantDE",
|          "tenantTag": null
|        }
|      ]
|    }
|}""".stripMargin

    val (application, retTargetEnvConf) = createAppAndGetConf(Some(INVALID_CUSTOM_TARGET_ENV_CONF))

    retTargetEnvConf shouldBe List.empty
    // TODO "JsonValidationError(List('previewUrlTemplate' is undefined on object"

    shutdownAppAndDb(application)

  }

  it should "result from a missing $QUERY placeholder in the preview URL template string" in {

    val INVALID_CUSTOM_TARGET_ENV_CONF = s"""{
|    "LIVE": {
|      "de": [
|        {
|          "rulesCollection": "MainTenantDE",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/de/main-tenant/search?query=$$QQUERY"
|        }
|      ]
|    }
|}""".stripMargin

    val (application, retTargetEnvConf) = createAppAndGetConf(Some(INVALID_CUSTOM_TARGET_ENV_CONF))

    retTargetEnvConf shouldBe List.empty
    // TODO "previewUrlTemplate does not contain $QUERY placeholder"

    shutdownAppAndDb(application)

  }

  it should "result in a WARN for missing PRELIVE instructions, when PRELIVE is activated" in {
    
    val PARTIALLY_VALID_CUSTOM_TARGET_ENV_CONF = s"""{
|    "LIVE": {
|      "de": [
|        {
|          "rulesCollection": "MainTenantDE",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/de/main-tenant/search?query=$$QUERY"
|        }
|      ]
|    }
|}""".stripMargin

    val (application, retTargetEnvConf) = createAppAndGetConf(
      Some(PARTIALLY_VALID_CUSTOM_TARGET_ENV_CONF),
      true
    )

    retTargetEnvConf shouldBe Seq(
      TargetEnvironmentInstance(
        "LIVE",
        Seq(
          TargetEnvironmentGroup(
            "de",
            Seq(
              TargetEnvironmentDescription(
                "MainTenantDE",
                None,
                "https://www.example.com/de/main-tenant/search?query=$QUERY"
              )
            )
          )
        )
      )
    )
    // TODO "[warn]" ... "In TargetEnvironmentConfigService :: read :: No PRELIVE target environment configuration for present, but necessary."

    shutdownAppAndDb(application)

  }

  "Target environment config for PRELIVE (JSON)" should "be ignored, if PRELIVE is deactivated (while outputting a WARN)" in {

    val PARTIALLY_VALID_CUSTOM_TARGET_ENV_CONF = s"""{
|    "LIVE": {
|      "de": [
|        {
|          "rulesCollection": "MainTenantDE",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/de/main-tenant/search?query=$$QUERY"
|        }
|      ]
|    },
|    "PRELIVE": {
|      "de": [
|        {
|          "rulesCollection": "MainTenantDE",
|          "tenantTag": null,
|          "previewUrlTemplate": "https://www.example.com/staging/de/main-tenant/search?query=$$QUERY"
|        }
|      ]
|    }
|}""".stripMargin    

    val (application, retTargetEnvConf) = createAppAndGetConf(Some(PARTIALLY_VALID_CUSTOM_TARGET_ENV_CONF))

    retTargetEnvConf shouldBe Seq(
      TargetEnvironmentInstance(
        "LIVE",
        Seq(
          TargetEnvironmentGroup(
            "de",
            Seq(
              TargetEnvironmentDescription(
                "MainTenantDE",
                None,
                "https://www.example.com/de/main-tenant/search?query=$QUERY"
              )
            )
          )
        )
      )
    )
    // TODO "[warn]" ... In TargetEnvironmentConfigService :: read :: PRELIVE target environment configuration present, but not necessary. Will be ignored.

    shutdownAppAndDb(application)

  }

  "Default target environment config" should "be properly translated to a JSON string (for interfacing with the frontend)" in {

    // TODO Test default target environment config within the application context (not only JSON conversion)

    import models.config.TargetEnvironment._
    import play.api.libs.json._

    Json.toJson(
      DEFAULT_TARGET_ENV_CONF_MODEL
    ).toString shouldBe """[{"id":"LIVE","targetEnvironmentGroups":[{"id":"en","targetEnvironments":[{"rulesCollection":"AmazonEN","previewUrlTemplate":"https://www.amazon.com/s?k=$QUERY"}]}]}]"""

  }

}