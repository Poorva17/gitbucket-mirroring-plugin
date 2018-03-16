package io.github.gitbucket.mirroring.service

import java.io.File
import java.net.URI
import java.util.Date

import gitbucket.core.util.Directory
import io.github.gitbucket.mirroring.model.{Mirror, MirrorStatus}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.{FetchResult, RefSpec}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.control.NonFatal

trait GitService {

  private val mirrorRefSpec = new RefSpec("+refs/*:refs/*")
  private val logger        = LoggerFactory.getLogger(classOf[MirrorService])

  def sync(mirror: Mirror): Mirror = {
    val mirrorStatus = Try {
      val repository = localRepository(mirror.userName, mirror.repositoryName)
      val remoteUrl  = URI.create(mirror.remoteUrl)
      fetch(new Git(repository), remoteUrl.toString)
      onSuccess(mirror)
    }.recover {
      case NonFatal(ex) => onFailure(mirror, ex)
    }.get
    mirror.withStatus(mirrorStatus)
  }

  private def onFailure(mirror: Mirror, throwable: Throwable): MirrorStatus = {
    val repositoryName = s"${mirror.userName}/${mirror.repositoryName}"
    val message        = s"Error while executing mirror status for repository $repositoryName: ${throwable.getMessage}"
    logger.error(message, throwable)
    MirrorStatus(new Date(System.currentTimeMillis()), successful = false, Some(throwable.getMessage))
  }

  private def onSuccess(mirror: Mirror): MirrorStatus = {
    logger.info(s"Mirror status has been successfully executed for repository ${mirror.userName}/${mirror.repositoryName}.")
    MirrorStatus(new Date(System.currentTimeMillis()), successful = true, None)
  }

  private def fetch(git: Git, remote: String): FetchResult = {
    git.fetch().setRemote(remote).setRefSpecs(Seq(mirrorRefSpec).asJava).call()
  }

  private def localRepository(owner: String, repositoryName: String): Repository = {
    val repositoryPath = s"${Directory.GitBucketHome}/repositories/$owner/$repositoryName.git"

    new FileRepositoryBuilder()
      .setGitDir(new File(repositoryPath))
      .readEnvironment()
      .findGitDir()
      .build()
  }
}
