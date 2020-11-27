package models.input

import org.scalatest.{FlatSpec, Matchers}

class InputValidatorSpec extends FlatSpec with Matchers {

  "InputValidator" should "validate invalid input terms correctly" in {

    InputValidator.validateInputTerm("") shouldBe Seq("Invalid empty input.")
    InputValidator.validateInputTerm("     ") shouldBe Seq("Invalid empty input.")
    InputValidator.validateInputTerm("\"") shouldBe Seq("Invalid input ('\"'). There must be at least one non-control character of Querqy present.")
    InputValidator.validateInputTerm("   \" ") shouldBe Seq("Invalid input ('   \" '). There must be at least one non-control character of Querqy present.")
    InputValidator.validateInputTerm("   \"\" ") shouldBe Seq("Invalid input ('   \"\" '). There must be at least one non-control character of Querqy present.")
    InputValidator.validateInputTerm("\"\"") shouldBe Seq("Invalid input ('\"\"'). There must be at least one non-control character of Querqy present.")
    InputValidator.validateInputTerm("\"*") shouldBe Seq("Invalid input ('\"*'). There must be at least one non-control character of Querqy present.")

  }

  "InputValidator" should "accept valid input terms correctly" in {

    InputValidator.validateInputTerm("\" \"") shouldBe Nil
    InputValidator.validateInputTerm("\"trouser\"") shouldBe Nil
    InputValidator.validateInputTerm("\"trouser*") shouldBe Nil
    InputValidator.validateInputTerm("\"trouser") shouldBe Nil
    InputValidator.validateInputTerm("trouser\"") shouldBe Nil

  }

}
