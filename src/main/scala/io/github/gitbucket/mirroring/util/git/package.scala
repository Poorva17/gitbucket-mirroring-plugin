package io.github.gitbucket.mirroring.util

import io.github.gitbucket.mirroring.util.git.command.PullMirrorCommand
import org.eclipse.jgit.api.Git

package object git {

  implicit class GitWrapper(git: Git) {

    def pullMirror(): PullMirrorCommand = new PullMirrorCommand(git.getRepository)

  }
}
