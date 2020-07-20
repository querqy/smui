import com.google.inject.AbstractModule

class SmuiStartModule extends AbstractModule {
  override def configure() = {
    bind(classOf[models.eventhistory.MigrationService]).asEagerSingleton()
  }
}
