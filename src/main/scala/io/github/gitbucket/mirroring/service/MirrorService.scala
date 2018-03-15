package io.github.gitbucket.mirroring.service

import gitbucket.core.model.Profile.profile.api._
import gitbucket.core.servlet.Database
import io.github.gitbucket.mirroring.model.Profile.{MirrorStatuses, Mirrors}
import io.github.gitbucket.mirroring.model.{Mirror, MirrorStatus, Profile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

trait MirrorService {
  private val db = Database()

  def selectBy(owner: String, repositoryName: String): Query[Profile.Mirrors, Mirror, Seq] = {
    Mirrors.filter { mirror => mirror.userName === owner.bind && mirror.repositoryName === repositoryName.bind }
  }

  def deleteMirrorByRepository(owner: String, repositoryName: String): Future[Boolean] = {
    db.run {
      selectBy(owner, repositoryName).delete.transactionally.map(_ > 0)
    }
  }

  def findMirrorByRepository(owner: String, repositoryName: String): Future[Option[Mirror]] = {
    db.run {
      selectBy(owner, repositoryName).result.headOption
    }
  }

  def findMirrorByRepositoryWithStatus(owner: String, repositoryName: String): Future[Option[(Mirror, Option[MirrorStatus])]] = {
    db.run {
      selectBy(owner, repositoryName).joinLeft(MirrorStatuses).on(_.id === _.mirrorId).result.headOption
    }
  }

  def insertOrUpdateMirrorUpdate(status: MirrorStatus): Future[MirrorStatus] = {
    db.run {
      MirrorStatuses.insertOrUpdate(status).map(_ => status).transactionally
    }
  }

  def insertMirror(mirror: Mirror): Future[Mirror] = {
    db.run {
      val insertQuery = Mirrors returning Mirrors.map(_.id) into ((m, id) => m.copy(id = Some(id)))
      (insertQuery += mirror).transactionally
    }
  }

  def updateMirror(newMirror: Mirror): Future[Option[Mirror]] = {
    val maybeId = Await.result(findMirrorByRepository(newMirror.userName, newMirror.repositoryName), 5.seconds).flatMap(_.id)
    val mirrorWithId = newMirror.copy(id = maybeId)
    db.run {
      selectBy(newMirror.userName, newMirror.repositoryName)
        .update(mirrorWithId)
        .transactionally
        .map { rowNumber => if (rowNumber == 0) None else Some(mirrorWithId) }
    }
  }
}
