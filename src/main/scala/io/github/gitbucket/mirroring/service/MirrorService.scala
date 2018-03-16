package io.github.gitbucket.mirroring.service

import gitbucket.core.service.RepositoryService.RepositoryInfo
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

  def findMirror(repo: RepositoryInfo): Option[Mirror]             = execute(mirrors => read(mirrors.get(makeKey(repo))))
  def deleteMirror(repo: RepositoryInfo): Option[Mirror]           = execute(mirrors => read(mirrors.remove(makeKey(repo))))
  def upsert(repo: RepositoryInfo, mirror: Mirror): Option[Mirror] = execute(mirrors => read(mirrors.put(makeKey(repo), write(mirror))))

  private def makeKey(repo: RepositoryInfo) = s"${repo.owner}-${repo.name}"

  private def read(string: String): Option[Mirror] = Option(string).map(Serialization.read[Mirror])
  private def write(mirror: Mirror): String        = Serialization.write(mirror)
}
