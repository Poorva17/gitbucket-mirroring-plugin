package io.github.gitbucket.mirroring.service

import java.io.File
import java.net.URI
import java.util.Date

import gitbucket.core.service.RepositoryService.RepositoryInfo
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

class GitService(repoInfo: RepositoryInfo, mirror: Mirror) {

  private val mirrorRefSpec  = new RefSpec("+refs/*:refs/*")
  private val logger         = LoggerFactory.getLogger(classOf[MirrorService])
  private val repositoryPath = s"${Directory.GitBucketHome}/repositories/${repoInfo.owner}/${repoInfo.name}.git"

  def sync(): Mirror = {
    val mirrorStatus = try {
      fetch()
      successStatus()
    } catch {
      case NonFatal(ex) => failureStatus(ex)
    }

    mirror.withStatus(mirrorStatus)
  }

  private def fetch(): FetchResult = {
    val remoteUrl    = URI.create(mirror.remoteUrl)
    val fetchCommand = git().fetch().setRemote(remoteUrl.toString).setRefSpecs(mirrorRefSpec)
    fetchCommand.call()
  }

  private def git(): Git = new Git(
    new FileRepositoryBuilder()
      .setGitDir(new File(repositoryPath))
      .readEnvironment()
      .findGitDir()
      .build()
  )

  private def successStatus(): MirrorStatus = {
    logger.info(s"Mirror status has been successfully executed for repository ${repoInfo.owner}/${repoInfo.name}.")
    MirrorStatus(new Date(System.currentTimeMillis()), successful = true, None)
  }

  private def failureStatus(throwable: Throwable): MirrorStatus = {
    val repositoryName = s"${repoInfo.owner}/${repoInfo.name}"
    val message        = s"Error while executing mirror status for repository $repositoryName: ${throwable.getMessage}"
    logger.error(message, throwable)
    MirrorStatus(new Date(System.currentTimeMillis()), successful = false, Some(throwable.getMessage))
  }
}
