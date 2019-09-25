package models

trait TestData {

  val indexDe = SolrIndex(name = "de", description = "German")
  val indexEn = SolrIndex(name = "en", description = "English")

}

object TestData extends TestData
