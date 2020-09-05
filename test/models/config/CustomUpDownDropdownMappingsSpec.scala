package models.config

import org.scalatest.{FlatSpec, Matchers}

import play.api.inject.Injector
import play.api.{Application, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.db.{Database, Databases}

import models.FeatureToggleModel._

class CustomUpDownDropdownMappingsSpec extends FlatSpec with Matchers {

  private val db: Database = Databases.inMemory()

  private val BASE_APP_CONFIG: Seq[(String, Any)] = Seq(
    "db.default.url" -> db.url,
    "db.default.driver" -> "org.h2.Driver",
    "db.default.username" -> "",
    "db.default.password" -> ""
  )

  private val TOGGLE_TO_TEST = "toggle.ui-concept.custom.up-down-dropdown-mappings"

  private val DEFAULT_MAPPINGS = "[{\"displayName\":\"UP(+++++)\",\"upDownType\":0,\"boostMalusValue\":500},{\"displayName\":\"UP(++++)\",\"upDownType\":0,\"boostMalusValue\":100},{\"displayName\":\"UP(+++)\",\"upDownType\":0,\"boostMalusValue\":50},{\"displayName\":\"UP(++)\",\"upDownType\":0,\"boostMalusValue\":10},{\"displayName\":\"UP(+)\",\"upDownType\":0,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(-)\",\"upDownType\":1,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(--)\",\"upDownType\":1,\"boostMalusValue\": 10},{\"displayName\":\"DOWN(---)\",\"upDownType\":1,\"boostMalusValue\": 50},{\"displayName\":\"DOWN(----)\",\"upDownType\":1,\"boostMalusValue\": 100},{\"displayName\":\"DOWN(-----)\",\"upDownType\":1,\"boostMalusValue\": 500}]"

  def applicationWithFeatureToggleService(additionalAppConfig: Seq[(String, Any)]) = {

    val allAppConfig: Seq[(String, Any)] =
      BASE_APP_CONFIG ++
        additionalAppConfig

    val application: Application = new GuiceApplicationBuilder().
      in(Mode.Test).
      configure(allAppConfig: _*).
      build()
    val injector: Injector = application.injector

    injector.instanceOf[FeatureToggleService]
  }

  "FeatureToggleService" should "deliver valid custom UP/DOWN dropdown mappings" in {
    val VALID_CUSTOM_MAPPINGS =
      "[{\"displayName\":\"UP(+++++)\",\"upDownType\":0,\"boostMalusValue\":750},{\"displayName\":\"UP(++++)\",\"upDownType\":0,\"boostMalusValue\":100},{\"displayName\":\"UP(+++)\",\"upDownType\":0,\"boostMalusValue\":50},{\"displayName\":\"UP(++)\",\"upDownType\":0,\"boostMalusValue\":10},{\"displayName\":\"UP(+)\",\"upDownType\":0,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(-)\",\"upDownType\":1,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(--)\",\"upDownType\":1,\"boostMalusValue\": 10},{\"displayName\":\"DOWN(---)\",\"upDownType\":1,\"boostMalusValue\": 50},{\"displayName\":\"DOWN(----)\",\"upDownType\":1,\"boostMalusValue\": 100},{\"displayName\":\"DOWN(-----)\",\"upDownType\":1,\"boostMalusValue\": 750}]"

    val featureToggleService = applicationWithFeatureToggleService(
      Seq(TOGGLE_TO_TEST -> VALID_CUSTOM_MAPPINGS)
    )

    val toTestValue = featureToggleService.getJsFrontendToggleList
      .filter(t => t.toggleName.equals(TOGGLE_TO_TEST))
      .head
      .toggleValue
      .render()

    toTestValue shouldEqual VALID_CUSTOM_MAPPINGS
  }

  "FeatureToggleService" should "reject invalid JSON and provide default UP/DOWN dropdown mappings" in {
    val BROKEN_JSON = "[{\"displayName\":\"UP(+++++)\",\"upDownType\":0,\"boostMalusValue\":750}"

    val featureToggleService = applicationWithFeatureToggleService(
      Seq(TOGGLE_TO_TEST -> BROKEN_JSON)
    )

    val toTestValue = featureToggleService.getJsFrontendToggleList
      .filter(t => t.toggleName.equals(TOGGLE_TO_TEST))
      .head
      .toggleValue
      .render()

    toTestValue shouldEqual DEFAULT_MAPPINGS
  }

  "FeatureToggleService" should "reject invalid custom UP/DOWN dropdown mappings with a wrong type and provide default" in {
    val INVALID_CUSTOM_MAPPINGS =
      "[{\"displayName\":\"UP(+++++)\",\"upDownType\":2,\"boostMalusValue\":750},{\"displayName\":\"UP(++++)\",\"upDownType\":0,\"boostMalusValue\":100},{\"displayName\":\"UP(+++)\",\"upDownType\":0,\"boostMalusValue\":50},{\"displayName\":\"UP(++)\",\"upDownType\":0,\"boostMalusValue\":10},{\"displayName\":\"UP(+)\",\"upDownType\":0,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(-)\",\"upDownType\":1,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(--)\",\"upDownType\":1,\"boostMalusValue\": 10},{\"displayName\":\"DOWN(---)\",\"upDownType\":1,\"boostMalusValue\": 50},{\"displayName\":\"DOWN(----)\",\"upDownType\":1,\"boostMalusValue\": 100},{\"displayName\":\"DOWN(-----)\",\"upDownType\":1,\"boostMalusValue\": 750}]"

    val featureToggleService = applicationWithFeatureToggleService(
      Seq(TOGGLE_TO_TEST -> INVALID_CUSTOM_MAPPINGS)
    )

    val toTestValue = featureToggleService.getJsFrontendToggleList
      .filter(t => t.toggleName.equals(TOGGLE_TO_TEST))
      .head
      .toggleValue
      .render()

    toTestValue shouldEqual DEFAULT_MAPPINGS
  }

  "FeatureToggleService" should "reject invalid custom UP/DOWN dropdown mappings without DOWNs and provide default" in {
    val UNPLAUSIBLE_CUSTOM_MAPPINGS =
      "[{\"displayName\":\"UP(+++++)\",\"upDownType\":0,\"boostMalusValue\":750},{\"displayName\":\"UP(++++)\",\"upDownType\":0,\"boostMalusValue\":100},{\"displayName\":\"UP(+++)\",\"upDownType\":0,\"boostMalusValue\":50},{\"displayName\":\"UP(++)\",\"upDownType\":0,\"boostMalusValue\":10},{\"displayName\":\"UP(+)\",\"upDownType\":0,\"boostMalusValue\": 5}]"

    val featureToggleService = applicationWithFeatureToggleService(
      Seq(TOGGLE_TO_TEST -> UNPLAUSIBLE_CUSTOM_MAPPINGS)
    )

    val toTestValue = featureToggleService.getJsFrontendToggleList
      .filter(t => t.toggleName.equals(TOGGLE_TO_TEST))
      .head
      .toggleValue
      .render()

    toTestValue shouldEqual DEFAULT_MAPPINGS
  }

  "FeatureToggleService" should "reject invalid custom UP/DOWN dropdown mappings without UPs and provide default" in {
    val UNPLAUSIBLE_CUSTOM_MAPPINGS =
      "[{\"displayName\":\"DOWN(-)\",\"upDownType\":1,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(--)\",\"upDownType\":1,\"boostMalusValue\": 10},{\"displayName\":\"DOWN(---)\",\"upDownType\":1,\"boostMalusValue\": 50},{\"displayName\":\"DOWN(----)\",\"upDownType\":1,\"boostMalusValue\": 100},{\"displayName\":\"DOWN(-----)\",\"upDownType\":1,\"boostMalusValue\": 750}]"

    val featureToggleService = applicationWithFeatureToggleService(
      Seq(TOGGLE_TO_TEST -> UNPLAUSIBLE_CUSTOM_MAPPINGS)
    )

    val toTestValue = featureToggleService.getJsFrontendToggleList
      .filter(t => t.toggleName.equals(TOGGLE_TO_TEST))
      .head
      .toggleValue
      .render()

    toTestValue shouldEqual DEFAULT_MAPPINGS
  }

}