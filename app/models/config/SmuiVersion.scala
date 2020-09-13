package models.config

import play.api.Logging

import scala.io.Source
import scala.util.Try
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json, Reads}

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
    val regExPattern = "([\\d]+).([\\d]+).([\\d]+)".r
    Try({
      val regExPattern(majorStr, minorStr, buildStr) = rawVersionString
      // convert strings to Ints
      SmuiVersion(
        majorStr.toInt, minorStr.toInt, buildStr.toInt
      )
    }).toOption match {
      case None => {
        logger.error(s":: parse :: failed to parse rawVersionString = $rawVersionString")
        None
      }
      case Some(version) => Some(version)
    }
  }

  def latestVersionFromDockerHub(): Option[SmuiVersion] = {
    Try({
      // request latest and previous version from hub.docker.com
      // TODO confirm we're dealing with a stable DockerHub API here!
      val LATEST_DOCKER_HUB_URL = "https://hub.docker.com/v2/repositories/querqy/smui/tags/?page_size=2&page=1"
      Source.fromURL(LATEST_DOCKER_HUB_URL).mkString
    }).toOption match {
      case None => None
      case Some(rawDockerHubResp) => {
        // TODO make any plausibility checks (maybe, that "results"(0) contains "latest")?
        def parseJsonResponse(jsonRead: Reads[String]): Option[String] = {
          Json.parse(rawDockerHubResp).validate[String](jsonRead) match {
            case JsSuccess(rawVer, _) => {
              val _: String = rawVer
              logger.info(s":: match :: rawVer = $rawVer")
              Some(rawVer)
            }
            case e: JsError => {
              logger.error(s":: error parsing latest DockerHub version JSON for SMUI (e = $e)")
              None
            }
          }
        }
        val jsonReadLatestVersionFromDockerHubResp1 = ((JsPath \ "results") (1) \ "name").read[String]
        val rawVer1 = parseJsonResponse(jsonReadLatestVersionFromDockerHubResp1)
        // TODO assume parsing works, might produce an exception
        val rawVer = (if(rawVer1.get.equals("latest")) {
          // hub.docker.com API does not seem to provide a stable interface to the latest version in 2nd JSON entry
          val jsonReadLatestVersionFromDockerHubResp0 = ((JsPath \ "results") (0) \ "name").read[String]
          val rawVer0 = parseJsonResponse(jsonReadLatestVersionFromDockerHubResp0)
          rawVer0.get
        } else {
          rawVer1.get
        })
        parse(rawVer) match {
          case None => {
            logger.error(s":: unable to parse latest DockerHub version string for SMUI (rawVer = $rawVer)")
            None
          }
          case Some(version) => Some(version)
        }
      }
    }
  }

}
