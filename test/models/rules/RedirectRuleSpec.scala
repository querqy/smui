package models.rules

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RedirectRuleSpec extends AnyFlatSpec with Matchers {

  val rule1 = RedirectRule(RedirectRuleId(), "target1", isActive = true)
  val rule2 = RedirectRule(RedirectRuleId(), "target2", isActive = false)

  "RedirectRule" should "create copy that has same values and a new Id" in {
    assertValues(rule1, RedirectRule.createWithNewIdFrom(rule1))
    assertValues(rule2, RedirectRule.createWithNewIdFrom(rule2))
  }

  def assertValues(expected: RedirectRule, actual: RedirectRule): Unit = {
    actual.id should not be expected.id
    actual.target shouldBe expected.target
    actual.isActive shouldBe expected.isActive
  }

}
