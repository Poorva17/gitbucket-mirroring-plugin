package csw.tools.mirroring.model

import java.util.Date

final case class Mirror(
    name: String,
    remoteUrl: String,
    enabled: Boolean,
    status: Option[MirrorStatus]
) {
  def withStatus(other: MirrorStatus): Mirror = copy(status = Some(other))
}

final case class MirrorStatus(
    date: Date,
    successful: Boolean,
    error: Option[String]
)
