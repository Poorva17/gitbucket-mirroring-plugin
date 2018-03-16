package io.github.gitbucket.mirroring.service

import org.h2.mvstore.{MVMap, MVStore}
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints}

case class EE(name: String)

class DD {
  implicit val formats: Formats = Serialization.formats(NoTypeHints)

  def dd() = {
    val fileName       = "/tmp/kv.mv.db"
    val store: MVStore = MVStore.open(fileName)

    try {
      val map: MVMap[String, String] = store.openMap("mirrors")
      val bytes                      = Serialization.write(EE("asdasda"))
      map.put("xyz", bytes)

      {
        val map: MVMap[String, String] = store.openMap("mirrors")
        println(Serialization.read[EE](map.get("xyz")))
      }

    } finally {
      store.close()
    }
  }
}
