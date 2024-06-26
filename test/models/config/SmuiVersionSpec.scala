package models.config

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SmuiVersionSpec extends AnyFlatSpec with Matchers {

  "SmuiVersion" should "correctly interpret equality" in {
    SmuiVersion(1, 0, 0) shouldEqual SmuiVersion(1, 0, 0)
    SmuiVersion(1, 1, 0) shouldEqual SmuiVersion(1, 1, 0)
    SmuiVersion(2, 1, 3) shouldEqual SmuiVersion(2, 1, 3)
  }

  it should "correctly interpret greaterThan versions" in {
    SmuiVersion(1, 0, 1).greaterThan(SmuiVersion(1, 0, 0)) shouldBe true
    SmuiVersion(1, 1, 0).greaterThan(SmuiVersion(1, 0, 1)) shouldBe true
    SmuiVersion(2, 0, 0).greaterThan(SmuiVersion(1, 1, 0)) shouldBe true
  }

  it should "correctly identify versions that are smaller" in {
    SmuiVersion(1, 0, 0).greaterThan(SmuiVersion(1, 0, 0)) shouldBe false
    SmuiVersion(1, 0, 0).greaterThan(SmuiVersion(1, 0, 1)) shouldBe false
    SmuiVersion(1, 0, 1).greaterThan(SmuiVersion(1, 0, 1)) shouldBe false
    SmuiVersion(1, 0, 1).greaterThan(SmuiVersion(1, 1, 0)) shouldBe false
    SmuiVersion(1, 1, 0).greaterThan(SmuiVersion(1, 1, 0)) shouldBe false
    SmuiVersion(1, 1, 0).greaterThan(SmuiVersion(1, 1, 1)) shouldBe false
    SmuiVersion(1, 1, 1).greaterThan(SmuiVersion(2, 0, 0)) shouldBe false
  }

  it should "parse valid version strings" in {
    SmuiVersion.parse("1.0.0") shouldEqual Some(SmuiVersion(1, 0, 0))
    SmuiVersion.parse("3.10.0") shouldEqual Some(SmuiVersion(3, 10, 0))
  }

  "SmuiVersion version parsing" should "return None for invalid strings" in {
    SmuiVersion.parse("invalid") shouldEqual None
    SmuiVersion.parse("1.0.") shouldEqual None
    SmuiVersion.parse("1.") shouldEqual None
  }

  it should "fill missing values for minor or build versions" in {
    SmuiVersion.parse("1") shouldEqual Some(SmuiVersion(1, 0, 0))
    SmuiVersion.parse("1.0") shouldEqual Some(SmuiVersion(1, 0, 0))
  }

  "SmuiVersion for next deployment" should "be greater than latest version provided on DockerHub" in {

    val latestDockerHub = SmuiVersion.latestVersionFromDockerHub()
    val current = SmuiVersion.parse(models.buildInfo.BuildInfo.version)

    current.get.greaterThan(latestDockerHub.get) shouldEqual true
  }

}
