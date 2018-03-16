package io.github.gitbucket.mirroring.service

import gitbucket.core.util.Directory
import io.github.gitbucket.mirroring.model.Mirror
import org.h2.mvstore.{MVMap, MVStore}
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints}

trait MirrorService {

  private implicit val formats: Formats = Serialization.formats(NoTypeHints)
  val fileName                          = s"${Directory.GitBucketHome}/kv.mv.db"
  val mapName                           = "mirrors"

  def execute[T](f: MVMap[String, String] => T): T = {
    val store: MVStore = MVStore.open(fileName)
    try {
      val mirrors: MVMap[String, String] = store.openMap(mapName)
      f(mirrors)
    } finally {
      store.close()
    }
  }

  def findMirror(owner: String, repositoryName: String): Option[Mirror] = execute { mirrors =>
    read(mirrors.get(makeKey(owner, repositoryName)))
  }

  def deleteMirror(owner: String, repositoryName: String): Option[Mirror] = execute { mirrors =>
    read(mirrors.remove(makeKey(owner, repositoryName)))
  }

  def upsert(mirror: Mirror): Option[Mirror] = execute { mirrors =>
    read(mirrors.put(makeKey(mirror.userName, mirror.repositoryName), Serialization.write(mirror)))
  }

  private def makeKey(owner: String, repositoryName: String) = s"$owner-$repositoryName"

  private def read(string: String): Option[Mirror] = Option(string).map(Serialization.read[Mirror])
}
