package controllers.auth

import play.api.inject.Injector
import play.api.mvc._
import play.api.{Configuration, Logging}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AuthActionFactory @Inject()(appConfig: Configuration, injector: Injector)
                                 (implicit ec: ExecutionContext) extends Logging {

  private lazy val authAction: Option[ActionBuilder[Request, AnyContent]] = {
    appConfig.getOptional[String]("smui.authAction")
      .filterNot(_.isBlank)
      .filterNot(_.trim.equals("scala.None"))
      .map { className =>
        logger.info(s"Configuring $className for authentication")
        injector.instanceOf(java.lang.Class.forName(className).asSubclass(classOf[ActionBuilder[Request, AnyContent]]))
      }
  }

  def getAuthenticatedAction(defaultAction: ActionBuilder[Request, AnyContent]): ActionBuilder[Request, AnyContent] = {
    authAction.getOrElse(defaultAction)
  }

}
