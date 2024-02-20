package modules

import com.google.inject.{AbstractModule, Provides}
import org.pac4j.core.client.{Client, Clients}
import org.pac4j.core.client.direct.AnonymousClient
import org.pac4j.core.config.Config
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http.client.direct.DirectBasicAuthClient
import org.pac4j.play.scala.{DefaultSecurityComponents, Pac4jScalaTemplateHelper, SecurityComponents}
import org.pac4j.play.store.{PlayCookieSessionStore, ShiroAesDataEncrypter}
import org.pac4j.play.{CallbackController, LogoutController}
import org.pac4j.saml.client.SAML2Client
import org.pac4j.saml.config.SAML2Configuration
import play.api.{Configuration, Environment}

import java.nio.charset.StandardCharsets

class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  import SecurityModule._

  private val baseUrl = configuration.get[String]("smui.auth.baseUrl")

  override def configure(): Unit = {
    val sKey = configuration.get[String]("play.http.secret.key").substring(0, 16)
    val dataEncrypter = new ShiroAesDataEncrypter(sKey.getBytes(StandardCharsets.UTF_8))
    val playSessionStore = new PlayCookieSessionStore(dataEncrypter)
    bind(classOf[SessionStore]).toInstance(playSessionStore)
    bind(classOf[SecurityComponents]).to(classOf[DefaultSecurityComponents])
    bind(classOf[Pac4jScalaTemplateHelper[CommonProfile]])

    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/")
    bind(classOf[CallbackController]).toInstance(callbackController)

    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl("/")
    bind(classOf[LogoutController]).toInstance(logoutController)
  }

  @Provides
  def provideConfig(): Config = {
    val maybeConfiguredClientName = configuration.getOptional[String](ConfigKeyAuthClient).filter(_.nonEmpty)
    val config: Option[Config] = maybeConfiguredClientName.map {
      case "DirectBasicAuthClient" => createConfiguredDirectBasicAuthConfig(s"$ConfigKeyPrefixClientConfig.ConfiguredDirectBasicAuthClient")
      case "SAML2Client" => createSaml2Config(s"$ConfigKeyPrefixClientConfig.SAML2Client")
      case other => throw new RuntimeException(s"Unsupported auth client config value: $other")
    }
    config match {
      case Some(config) => config
      case None => throw new RuntimeException(s"Failed configuring auth client")
    }
  }

  private def createConfiguredDirectBasicAuthConfig(keyPrefix: String): Config = {
    val username = configuration.get[String](s"$keyPrefix.username")
    val password = configuration.get[String](s"$keyPrefix.password")
    new Config(new DirectBasicAuthClient(ConfiguredBasicAuthAuthenticator(username, password)))
  }

  private def createSaml2Config(keyPrefix: String): Config = {
    val cfg = new SAML2Configuration(
      configuration.get[String](s"$keyPrefix.keystore"),
      configuration.get[String](s"$keyPrefix.keystorePassword"),
      configuration.get[String](s"$keyPrefix.privateKeyPassword"),
      configuration.get[String](s"$keyPrefix.identityProviderMetadataPath")
    )
    cfg.setServiceProviderEntityId(configuration.get[String](s"$keyPrefix.serviceProviderEntityId"))
    cfg.setServiceProviderMetadataPath(configuration.get[String](s"$keyPrefix.serviceProviderMetadataPath"))
    cfg.setMaximumAuthenticationLifetime(configuration.get[Long](s"$keyPrefix.maximumAuthenticationLifetime"))
    val allClients = Option(new SAML2Client(cfg)).toSeq :+ new AnonymousClient()
    // callback URL path as configured in `routes`
    val clients = new Clients(s"$baseUrl/callback", allClients:_*)
    new Config(clients)
  }

}

object SecurityModule {
  val ConfigKeyAuthClient = "smui.auth.client"
  val ConfigKeyPrefixClientConfig = "smui.auth.clients"
}
