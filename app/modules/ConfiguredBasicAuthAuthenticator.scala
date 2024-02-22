package modules

import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.credentials.{Credentials, UsernamePasswordCredentials}
import org.pac4j.core.credentials.authenticator.Authenticator
import org.pac4j.core.exception.CredentialsException
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.util.{CommonHelper, Pac4jConstants}


case class ConfiguredBasicAuthAuthenticator(validUserId: String, validPassword: String) extends Authenticator {

  override def validate(credentials: Credentials, context: WebContext, sessionStore: SessionStore): Unit = {
    if (credentials == null) throw new CredentialsException("No credential")
    val userCredentials = credentials.asInstanceOf[UsernamePasswordCredentials]
    val username = userCredentials.getUsername()
    val password = userCredentials.getPassword()
    if (CommonHelper.isBlank(username)) throw new CredentialsException("Username cannot be blank")
    if (CommonHelper.isBlank(password)) throw new CredentialsException("Password cannot be blank")
    if (CommonHelper.areNotEquals(username.toLowerCase, validUserId.toLowerCase)) throw new CredentialsException("Username : '" + username + "' does not match valid user")
    if (CommonHelper.areNotEquals(password, validPassword)) throw new CredentialsException("Password does not match valid password")
    val profile = new CommonProfile
    profile.setId(username)
    profile.addAttribute(Pac4jConstants.USERNAME, username)
    userCredentials.setUserProfile(profile)
  }

}

