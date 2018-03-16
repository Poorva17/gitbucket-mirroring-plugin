package io.github.gitbucket.mirroring.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import io.github.gitbucket.mirroring.service.MirrorService

class MirrorController(mirrorService: MirrorService)
    extends ControllerBase
    with AccountService
    with OwnerAuthenticator
    with RepositoryService {

  get("/:owner/:repository/mirror") {
    ownerOnly(repo => gitbucket.mirror.html.list(mirrorService.findMirror(repo).toList, repo))
  }

  get("/:owner/:repository/mirror/new") {
    ownerOnly(repo => gitbucket.mirror.html.create(repo))
  }

  get("/:owner/:repository/mirror/edit") {
    ownerOnly(repo => mirrorService.findMirror(repo).map(mirror => gitbucket.mirror.html.mirror(mirror, repo)).getOrElse(NotFound()))
  }
}
