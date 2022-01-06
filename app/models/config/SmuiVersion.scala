package models.config

import play.api.Logging

import scala.io.Source
import scala.util.Try
import play.api.libs.json.{JsError, JsPath, JsArray, JsSuccess, Json, Reads}

case class SmuiVersion(
  major: Int,
  minor: Int,
  build: Int
) {

  def greaterThan(bVersion: SmuiVersion) = {
    if (major.equals(bVersion.major)) {
      // compare minor
      if (minor.equals(bVersion.minor)) {
        // compare build
        build > bVersion.build
      } else {
        minor > bVersion.minor
      }
    } else {
      major > bVersion.major
    }
  }

  override def toString: String = {
    s"$major.$minor.$build"
  }

}

object SmuiVersion extends Logging {

  def parse(rawVersionString: String): Option[SmuiVersion] = {
    val Pattern = """^(\d+)(.\d+)?(.\d+)?$""".r
    rawVersionString match {
      case Pattern(major, null, null) => Some(SmuiVersion(major.toInt, 0, 0))
      case Pattern(major, minor, null) => Some(SmuiVersion(major.toInt, minor.tail.toInt, 0))
      case Pattern(major, minor, build) => Some(SmuiVersion(major.toInt, minor.tail.toInt, build.tail.toInt))
      case _ => None
    }
  }

  def latestVersionFromDockerHub(): Option[SmuiVersion] = {
    Try({
      // request latest and previous version from hub.docker.com
      // TODO confirm we're dealing with a stable DockerHub API here!
      val LATEST_DOCKER_HUB_URL = "https://hub.docker.com/v2/repositories/querqy/smui/tags/?page_size=4&page=1&ordering=last_updated"
      Source.fromURL(LATEST_DOCKER_HUB_URL).mkString
    }).toOption match {
      case None => None
      case Some(rawDockerHubResp) => {

        // Convert results to JSON array
        val jsonReadDockerHubVersionResults = (JsPath \ "results").read[JsArray]
        Json.parse(rawDockerHubResp).validate[JsArray](jsonReadDockerHubVersionResults) match {
          case JsSuccess(versionResults, _) => {
            logger.info(s":: Successfully parsed version number")

            // Extract version numbers
            val allLatestVersions = versionResults.value.map(versionEntry => {
              (versionEntry \ "name").validate[String] match {
                case JsSuccess(versionName, _) => Some(versionName)
                case e: JsError => {
                  logger.error(s":: error parsing latest DockerHub version JSON for SMUI (e = $e)")
                  None
                }
              }
            }).flatten

            // Plausibility check
            if (!allLatestVersions.contains("latest")) {
              logger.error(s":: allLatestVersions does not contain a 'latest' version. This is unexpected. Please contact the author.")
            }

            val specificVersions = allLatestVersions.filter(version => {
              val patternFullVersion: scala.util.matching.Regex = """\d+.\d+.\d+""".r
              patternFullVersion.findFirstMatchIn(version).isDefined
            })

            // Next plausibility check
            if (specificVersions.size != 1) {
              logger.error(s":: more or less than 1 specificVersions found (specificVersions = >>>${specificVersions}<<<) This is unexpected. Please contact the author.")
            }

            SmuiVersion.parse(specificVersions.head)

          }
          case e: JsError => {
            logger.error(s":: error parsing latest DockerHub version JSON for SMUI (e = $e)")
            None
          }
        }
      }
    }
  }

}
