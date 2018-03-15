package io.github.gitbucket.mirroring.service

import java.io.File
import java.net.URI
import java.util.Date

import gitbucket.core.model.Profile.profile.api._
import gitbucket.core.servlet.Database
import gitbucket.core.util.Directory
import io.github.gitbucket.mirroring.model.Profile.{MirrorStatuses, Mirrors}
import io.github.gitbucket.mirroring.model.{Mirror, MirrorStatus, Profile}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}
import scala.util.Try

trait MirrorService {

  private val logger = LoggerFactory.getLogger(classOf[MirrorService])

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

  def executeMirrorUpdate(mirror: Mirror): Future[MirrorStatus] = {

    def onFailure(throwable: Throwable): MirrorStatus = {

      val repositoryName = s"${mirror.userName}/${mirror.repositoryName}"
      val message = s"Error while executing mirror status for repository $repositoryName: ${throwable.getMessage}"

      logger.error(message, throwable)

      MirrorStatus(mirror.id.get, new Date(System.currentTimeMillis()), successful = false, Some(throwable.getMessage))
    }

    def onSuccess(): MirrorStatus = {
      logger.info(
        s"Mirror status has been successfully executed for repository ${mirror.userName}/${mirror.repositoryName}."
      )

      MirrorStatus(mirror.id.get, new Date(System.currentTimeMillis()), successful = true, None)
    }

    // Execute the push, get the result and convert it to a mirror status.

    val result = for {
      repository <- localRepository(mirror.userName, mirror.repositoryName)
      remoteUrl <- Try(URI.create(mirror.remoteUrl))
      pullMirrorCommand = new PullMirrorCommand(new Git(repository).getRepository).setRemote(remoteUrl.toString)
      _ <- Try { pullMirrorCommand.call() }
    } yield ()

    val status = result.fold(onFailure, _ => onSuccess())

    // Save the status.

    insertOrUpdateMirrorUpdate(status)
  }

  private def localRepository(owner: String, repositoryName: String): Try[Repository] = Try {

    val repositoryPath = s"${Directory.GitBucketHome}/repositories/$owner/$repositoryName.git"

    new FileRepositoryBuilder()
      .setGitDir(new File(repositoryPath))
      .readEnvironment()
      .findGitDir()
      .build()
  }


}
