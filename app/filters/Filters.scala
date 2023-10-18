package filters

import modules.SecurityModule
import org.pac4j.play.filters.SecurityFilter
import play.api.Configuration
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter

import javax.inject.Inject

class Filters @Inject()(configuration: Configuration, securityFilter: SecurityFilter) extends HttpFilters {

  override def filters: Seq[EssentialFilter] = {
    // The securityFilter applies the pac4j security rules. It is only used if a non-empty authentication
    // client has been configured.
    if (configuration.getOptional[String](SecurityModule.ConfigKeyAuthClient).exists(_.nonEmpty)) {
      Seq(securityFilter)
    } else {
      Seq.empty
    }
  }

}
