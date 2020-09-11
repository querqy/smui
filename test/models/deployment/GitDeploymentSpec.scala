package models.deployment

import org.scalatest.{FlatSpec, Matchers}
import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.testcontainers.containers.wait.strategy.Wait
import models.ApplicationTestBase
import org.testcontainers.containers.BindMode

// TODO consider moving spec & class to a service (instead of models)
class GitDeploymentSpec extends FlatSpec with Matchers with ApplicationTestBase with ForAllTestContainer {

  // TODO create temp path and files s"$TMP_PATH/git-server-keys" and s"$TMP_PATH/git-server-repos"
  val TMP_PATH = "./tmp-GitDeploymentSpec"

  override val container = GenericContainer(
    "jkarlos/git-server-docker", // see https://hub.docker.com/r/jkarlos/git-server-docker/
    exposedPorts = Seq(22),
    classpathResourceMapping = List(
      // TODO deal with environments not providing a temp directory
      (s"$TMP_PATH/git-server-keys", "/git-server/keys", BindMode.READ_ONLY),
      (s"$TMP_PATH/git-server-repos", "/git-server/repos", BindMode.READ_WRITE)
    ),
    waitStrategy = Wait.forListeningPort()
  )

  override protected lazy val additionalAppConfig: Seq[(String, Any)] = List(
    "XXX" -> "XXX"
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    createTestCores()
    createTestSpellings()
    createTestRule()
  }

  "SMUI" should "deploy all rules.txt files to a git repo (if configured)" in {



  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    // TODO delete temp folders and files
  }

}
