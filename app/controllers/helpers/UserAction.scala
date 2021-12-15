package controllers.helpers

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject
import models.{SearchManagementRepository, SessionDAO, User}
import play.api.Logger.logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class UserRequest[A](val user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

class UserAction @Inject()(val parser: BodyParsers.Default, searchManagementRepository: SearchManagementRepository)(implicit val executionContext: ExecutionContext)
  extends ActionBuilder[UserRequest, AnyContent]
    with ActionTransformer[Request, UserRequest] {

  def transform[A](request: Request[A]) = Future.successful {

    val sessionTokenOpt = request.session.get("sessionToken")
    val user = sessionTokenOpt
      .flatMap(token => SessionDAO.getSession(token))
      .filter(_.expiration.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
      .map(_.tokenData)
      .flatMap(searchManagementRepository.lookupUserByEmail)
    logger.info("User action transform: user=" + user + " (token=" + sessionTokenOpt + ") request= " + request.headers)
    new UserRequest(user, request)
  }
}