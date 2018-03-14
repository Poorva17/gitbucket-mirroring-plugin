package io.github.gitbucket.mirror.util.git.command

import org.eclipse.jgit.api.{Git, TransportCommand}
import org.eclipse.jgit.lib.{Constants, Repository}
import org.eclipse.jgit.transport.RefSpec

import scala.collection.JavaConverters._

class PullMirrorCommand(repo: Repository) extends TransportCommand[PullMirrorCommand, Unit](repo) {

  private var remote: String = Constants.DEFAULT_REMOTE_NAME

  def getRemote: String = remote

  def setRemote(remote: String): PullMirrorCommand = {
    this.remote = remote
    this
  }

  override def call(): Unit = {

    val mirrorRefSpec = new RefSpec("+refs/*:refs/*")
    val git = new Git(repo)

    // The mirror ref spec is not enough to propagate local deleted references. Therefore we use the remote ls
    // command to find all the deleted ref specs and add them explicitly.

    val lsRemoteCommand = git.lsRemote()
      .setRemote(remote)
      .setHeads(true)
      .setTags(true)

    configure(lsRemoteCommand)

    val deletedRefSpecs = lsRemoteCommand.call()
      .asScala
      .withFilter { ref => repo.findRef(ref.getName) == null }
      .map { ref => new RefSpec(s":${ref.getName}") }

    val pullCommand = git.fetch()
      .setRemote(remote)
      .setRefSpecs(((mirrorRefSpec :: Nil) ++ deletedRefSpecs).asJava)


    configure(pullCommand)

    pullCommand.call()
  }

}
