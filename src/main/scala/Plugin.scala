import gitbucket.core.controller.Context
import gitbucket.core.plugin.{Link, PluginRegistry}
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.service.SystemSettingsService
import io.github.gitbucket.mirroring.controller.{MirrorApiController, MirrorController}
import io.github.gitbucket.mirroring.service.MirrorService
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version
import javax.servlet.ServletContext

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String    = "mirroring"
  override val pluginName: String  = "Mirroring Plugin"
  override val description: String = "A Gitbucket plugin for pull based repository mirroring"

  override val versions: List[Version] = List(
    new Version("1.0.0", new LiquibaseMigration("update/gitbucket-mirror_1.0.0.xml"))
  )

  override val assetsMappings = Seq("/mirror" -> "/gitbucket/mirror/assets")

  private val mirrorService = new MirrorService

  override val controllers = Seq(
    "/*"      -> new MirrorController(mirrorService),
    "/api/v3" -> new MirrorApiController(mirrorService)
  )

  override val repositoryMenus = Seq(
    (repository: RepositoryInfo, context: Context) => Some(Link("mirror", "Mirror", "/mirror", Some("mirror")))
  )

  override def shutdown(registry: PluginRegistry, context: ServletContext, settings: SystemSettingsService.SystemSettings): Unit = {
    mirrorService.close()
    super.shutdown(registry, context, settings)
  }
}
