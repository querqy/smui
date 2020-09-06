package models.config

case class SmuiVersion(
  major: Int,
  minor: Int,
  build: Int
)

object SmuiVersion {

  def parse(verString: String): SmuiVersion = {
    // TODO
    SmuiVersion(
      1, 0, 0
    )
  }

}
