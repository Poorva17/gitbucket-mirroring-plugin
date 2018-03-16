package io.github.gitbucket.mirroring.service

import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.util.Directory
import io.github.gitbucket.mirroring.model.Mirror
import org.h2.mvstore.{MVMap, MVStore}
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints}

class MirrorService {
  private implicit val formats: Formats = Serialization.formats(NoTypeHints)
  private val fileName                  = s"${Directory.GitBucketHome}/kv.mv.db"

  private lazy val store: MVStore                 = MVStore.open(fileName)
  private lazy val mirrors: MVMap[String, String] = store.openMap("mirrors")

  def close(): Unit = store.close()

  def findMirror(repo: RepositoryInfo): Option[Mirror]             = read(mirrors.get(makeKey(repo)))
  def deleteMirror(repo: RepositoryInfo): Option[Mirror]           = read(mirrors.remove(makeKey(repo)))
  def upsert(repo: RepositoryInfo, mirror: Mirror): Option[Mirror] = read(mirrors.put(makeKey(repo), Serialization.write(mirror)))

  private def makeKey(repo: RepositoryInfo) = s"${repo.owner}-${repo.name}"

  private def read(string: String): Option[Mirror] = Option(string).map(Serialization.read[Mirror])
}
