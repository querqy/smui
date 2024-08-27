package models.rules

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FilterRuleSpec extends AnyFlatSpec with Matchers {

  val rule1 = FilterRule(FilterRuleId(), "term1", isActive = true)
  val rule2 = FilterRule(FilterRuleId(), "term2", isActive = false)

  "FilterRule" should "create copy that has same values and a new Id" in {
    assertValues(rule1, FilterRule.createWithNewIdFrom(rule1))
    assertValues(rule2, FilterRule.createWithNewIdFrom(rule2))
  }

  def assertValues(expected: FilterRule, actual: FilterRule): Unit = {
    actual.id should not be expected.id
    actual.term shouldBe expected.term
    actual.isActive shouldBe expected.isActive
  }

}
