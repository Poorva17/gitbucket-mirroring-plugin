package io.github.gitbucket.mirror.util

import io.github.gitbucket.mirror.util.git.command.{PullMirrorCommand, PushMirrorCommand}
import org.eclipse.jgit.api.Git

package object git {

  implicit class GitWrapper(git: Git) {

    def pushMirror(): PushMirrorCommand = new PushMirrorCommand(git.getRepository)
    def pullMirror(): PullMirrorCommand = new PullMirrorCommand(git.getRepository)

  }
}
