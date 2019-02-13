package controllers.auth

import javax.inject.Inject

import play.api.Configuration
import play.api.mvc._

import scala.concurrent.ExecutionContext

class AuthActionFactory @Inject()(parser: BodyParsers.Default, appConfig: Configuration)(implicit ec: ExecutionContext) {

  private val logger = play.api.Logger

  private def instantiateAuthAction(strClazz: String, defaultAction: ActionBuilder[MessagesRequest, AnyContent]): ActionBuilder[MessagesRequest, AnyContent] = {
    try {

      // TODO if possible instanciate authenticatedAction only once, not with every controller call

      def instantiate(clazz: java.lang.Class[_])(args: AnyRef*): AnyRef = {
        val constructor = clazz.getConstructors()(0)
        constructor.newInstance(args: _*).asInstanceOf[AnyRef]
      }

      val authenticatedAction = instantiate(
        java.lang.Class.forName(strClazz)
      )(parser, appConfig, ec)

      logger.debug(":: having instanciated " + authenticatedAction.toString)

      authenticatedAction.asInstanceOf[ActionBuilder[MessagesRequest, AnyContent]]

    } catch {
      case e: Throwable =>
        // TODO consider stop serving requests, if an expection during bootstrap of authAction happened. DO NOT return the defaultAction.

        logger.error(":: Exception during instantiation of smui.authAction :: " + e.getMessage)
        logger.error(":: Authentication protection IS NOT ACTIVE!")
        defaultAction
    }
  }

  def getAuthenticatedAction(defaultAction: ActionBuilder[MessagesRequest, AnyContent]): ActionBuilder[MessagesRequest, AnyContent] = {
    appConfig.getOptional[String]("smui.authAction") match {
      case Some(strClazz: String) =>
        if(strClazz.trim().equals("scala.None")) defaultAction
        else instantiateAuthAction(strClazz, defaultAction)
      case None =>
        defaultAction
    }
  }

}
