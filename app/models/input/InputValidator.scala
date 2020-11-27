package models.input

object InputValidator {

  private def validateNoEmptyTerm(term: String): Option[String] = {
    if(term.trim.isEmpty) {
      Some(s"Invalid empty input.")
    }
    // there must be at least one character present, that is not a Querqy control character
    else if(!term.trim.exists(p => !"\"*".contains(p))) {
      Some(s"Invalid input ('$term'). There must be at least one non-control character of Querqy present.")
    } else {
      None
    }
  }

  def validateInputTerm(term: String): Seq[String] = {
    Seq(
      validateNoEmptyTerm(term)
    ).flatten
  }

}
