package io.github.gitbucket.mirroring.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import io.github.gitbucket.mirroring.service.MirrorService

class MirrorController
    extends ControllerBase
    with AccountService
    with MirrorService
    with OwnerAuthenticator
    with RepositoryService {

  get("/:owner/:repository/mirror") {
    ownerOnly { repository =>
      gitbucket.mirror.html.list(findMirror(repository.owner, repository.name).toList, repository)

    }
  }

  get("/:owner/:repository/mirror/new") {
    ownerOnly { repository =>
      gitbucket.mirror.html.create(repository)
    }
  }

  get("/:owner/:repository/mirror/edit") {
    ownerOnly { repository =>
      (for {
        owner          <- params.getAs[String]("owner")
        repositoryName <- params.getAs[String]("repository")
        mirror         <- findMirror(owner, repositoryName)
      } yield {
        gitbucket.mirror.html.mirror(mirror, repository)
      }).getOrElse(NotFound())
    }
  }
}
