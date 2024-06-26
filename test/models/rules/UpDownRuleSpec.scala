package models.rules

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UpDownRuleSpec extends AnyFlatSpec with Matchers {

  val rule1 = UpDownRule(UpDownRuleId(), UpDownRule.TYPE_DOWN, 100, "term1", isActive = true)
  val rule2 = UpDownRule(UpDownRuleId(), UpDownRule.TYPE_UP, 200, "term1", isActive = false)

  "DeleteRule" should "create copy that has same values and a new Id" in {
    assertValues(rule1, UpDownRule.createWithNewIdFrom(rule1))
    assertValues(rule2, UpDownRule.createWithNewIdFrom(rule2))
  }

  def assertValues(expected: UpDownRule, actual: UpDownRule): Unit = {
    actual.id should not be expected.id
    actual.term shouldBe expected.term
    actual.isActive shouldBe expected.isActive
  }

}
