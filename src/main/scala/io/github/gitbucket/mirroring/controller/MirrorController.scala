package io.github.gitbucket.mirroring.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import io.github.gitbucket.mirroring.service.MirrorService
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class MirrorController extends ControllerBase
  with AccountService
  with MirrorService
  with OwnerAuthenticator
  with RepositoryService {

  private val logger = LoggerFactory.getLogger(classOf[MirrorController])

  get("/:owner/:repository/mirror")(ownerOnly { repository =>

    val mirrorsWithUpdate = Await.result(
      findMirrorByRepositoryWithStatus(repository.owner, repository.name),
      60.seconds
    )
    gitbucket.mirror.html.list(Seq(mirrorsWithUpdate), repository)

  })

  get("/:owner/:repository/mirror/new")(ownerOnly { repository =>
    gitbucket.mirror.html.create(repository)
  })

  get("/:owner/:repository/mirror/edit")(ownerOnly { repository =>
    (for {
      owner <- params.getAs[String]("owner")
      repositoryName <- params.getAs[String]("repository")
    } yield {
        val (mirror, maybeStatus) = Await.result(findMirrorByRepositoryWithStatus(owner, repositoryName), 60.seconds)
        gitbucket.mirror.html.mirror(mirror, maybeStatus, repository)
      }).getOrElse(NotFound())
  })
}
