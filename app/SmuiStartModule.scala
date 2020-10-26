import com.google.inject.AbstractModule
import services.MigrationService

class SmuiStartModule extends AbstractModule {
  override def configure() = {
    bind(classOf[MigrationService]).asEagerSingleton()
  }
}
