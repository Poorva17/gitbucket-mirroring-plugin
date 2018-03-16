package io.github.gitbucket.mirroring.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import io.github.gitbucket.mirroring.model.Mirror
import io.github.gitbucket.mirroring.service.{GitService, MirrorService}
import org.scalatra._

import scala.util.Try

class MirrorApiController extends ControllerBase with AccountService with MirrorService with OwnerAuthenticator with RepositoryService {

  delete("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly(repo => deleteMirror(repo).getOrElse(NotFound()))
  }

  get("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly(repo => findMirror(repo).getOrElse(NotFound()))
  }

  post("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly { repo =>
      Try {
        val mirror = parsedBody.extract[Mirror]
        upsert(repo, mirror)
        val location = s"${context.path}/api/v3/${repo.owner}/${repo.name}/mirror"
        Created(mirror, Map("location" -> location))
      }.getOrElse(BadRequest())
    }
  }

  put("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly { repo =>
      Try {
        val mirror = parsedBody.extract[Mirror]
        upsert(repo, mirror)
        Ok(mirror)
      }.getOrElse(NotFound())
    }
  }

  put("/api/v3/repos/:owner/:repository/mirror/status") {
    ownerOnly { repo =>
      val maybeResult = for {
        mirror <- findMirror(repo)
        mirrorWithStatus = new GitService(repo, mirror).sync()
        _      <- upsert(repo, mirrorWithStatus)
        status <- mirrorWithStatus.status
      } yield {
        Ok(status)
      }

      maybeResult.getOrElse(NotFound())
    }
  }
}
