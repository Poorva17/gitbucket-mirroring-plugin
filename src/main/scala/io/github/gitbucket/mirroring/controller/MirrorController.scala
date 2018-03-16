package io.github.gitbucket.mirroring.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import io.github.gitbucket.mirroring.service.MirrorService

class MirrorController extends ControllerBase with AccountService with MirrorService with OwnerAuthenticator with RepositoryService {

  get("/:owner/:repository/mirror") {
    ownerOnly(repo => gitbucket.mirror.html.list(findMirror(repo).toList, repo))
  }

  get("/:owner/:repository/mirror/new") {
    ownerOnly(repo => gitbucket.mirror.html.create(repo))
  }

  get("/:owner/:repository/mirror/edit") {
    ownerOnly(repo => findMirror(repo).map(mirror => gitbucket.mirror.html.mirror(mirror, repo)).getOrElse(NotFound()))
  }
}
