import gitbucket.core.controller.Context
import gitbucket.core.plugin.Link
import gitbucket.core.service.RepositoryService.RepositoryInfo
import io.github.gitbucket.mirroring.controller.{MirrorApiController, MirrorController}
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "mirroring"
  override val pluginName: String = "Mirroring Plugin"
  override val description: String = "A Gitbucket plugin for pull based repository mirroring"

  override val versions: List[Version] = List(
    new Version("1.0.0", new LiquibaseMigration("update/gitbucket-mirror_1.0.0.xml"))
  )

  override val assetsMappings = Seq("/mirror" -> "/gitbucket/mirror/assets")

  override val controllers = Seq(
    "/*" -> new MirrorController(),
    "/api/v3" -> new MirrorApiController()
  )

  override val repositoryMenus = Seq(
    (repository: RepositoryInfo, context: Context) => Some(Link("mirror", "Mirror", "/mirror", Some("mirror")))
  )
}
