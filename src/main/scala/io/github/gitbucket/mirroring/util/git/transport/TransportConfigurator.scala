package io.github.gitbucket.mirroring.util.git.transport

import java.io.IOException
import java.net.URI
import org.eclipse.jgit.api.TransportCommand

import scala.util.{Success, Try}


trait TransportConfigurator[C <: TransportCommand[C, _]] {

  def apply(command: C): Try[C]

}

object TransportConfigurator {

  def apply[C <: TransportCommand[C, _]](remoteUrl: URI): TransportConfigurator[C] = {
        new TransportConfigurator[C] {
          override def apply(command: C): Try[C] = Success(command)
    }
  }

}


