package models.`export`

class Exporter {

    def getAllSomethings(): Seq[Something] = {
      val something1 : Something = new Something("something1")
      val something2 : Something = new Something("something2")
      val x : Seq[Something] = Seq(something1,something2)
      x
    }

}
