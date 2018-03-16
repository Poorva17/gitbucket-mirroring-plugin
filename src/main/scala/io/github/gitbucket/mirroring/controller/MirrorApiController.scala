package io.github.gitbucket.mirroring.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.OwnerAuthenticator
import io.github.gitbucket.mirroring.model.Mirror
import io.github.gitbucket.mirroring.service.{GitService, MirrorService}
import org.scalatra._

import scala.util.Try

class MirrorApiController
    extends ControllerBase
    with AccountService
    with MirrorService
    with GitService
    with OwnerAuthenticator
    with RepositoryService {

  delete("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly { _ =>
      (for {
        owner      <- params.getAs[String]("owner")
        repository <- params.getAs[String]("repository")
        _          <- deleteMirror(owner, repository)
      } yield {
        NoContent()
      }).getOrElse(NotFound())
    }
  }

  get("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly { repository =>
      findMirror(repository.owner, repository.name).getOrElse(NotFound())
    }
  }

  post("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly { repository =>
      Try(parsedBody.extract[Mirror])
        .map { mirror =>
          upsert(mirror)
          val location = s"${context.path}/api/v3/${repository.owner}/${repository.name}/mirror"
          Created(mirror, Map("location" -> location))
        }
        .getOrElse(BadRequest())

    }
  }

  put("/api/v3/repos/:owner/:repository/mirror") {
    ownerOnly { _ =>
      val result = for {
        owner          <- params.getAs[String]("owner").toRight(NotFound())
        repositoryName <- params.getAs[String]("repository").toRight(NotFound())
        mirror         <- Try(parsedBody.extract[Mirror]).fold[Either[ActionResult, Mirror]](_ => Left(BadRequest()), Right(_))
        _              <- upsert(mirror).toRight(NotFound())
      } yield Ok(mirror)

      result.merge
    }
  }

  put("/api/v3/repos/:owner/:repository/mirror/status") {
    ownerOnly { repository =>
      (for {
        owner          <- params.getAs[String]("owner")
        repositoryName <- params.getAs[String]("repository")
        mirror         <- findMirror(owner, repositoryName)
        mirrorWithStatus = sync(mirror)
        _      <- upsert(mirrorWithStatus)
        status <- mirrorWithStatus.status
      } yield {
        Ok(status)
      }).getOrElse(NotFound())
    }
  }

}
