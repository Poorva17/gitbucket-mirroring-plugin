package io.github.gitbucket.mirroring.hook

import gitbucket.core.model.Profile
import gitbucket.core.plugin.ReceiveHook
import io.github.gitbucket.mirroring.service.MirrorService
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

class MirrorPostReceiveHook extends ReceiveHook with MirrorService {

  private val logger = LoggerFactory.getLogger(classOf[MirrorPostReceiveHook])

  override def postReceive(
    owner: String,
    repository: String,
    receivePack: ReceivePack,
    command: ReceiveCommand,
    pusher: String
  )(implicit session: Profile.profile.api.Session): Unit = {

    findMirrorByRepository(owner, repository).map { mirrors =>

      mirrors.foreach { mirror => if (mirror.enabled) executeMirrorUpdate(mirror) }
    }

  }

}
