package models.rules

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DeleteRuleSpec extends AnyFlatSpec with Matchers {

  val rule1 = DeleteRule(DeleteRuleId(), "term1", isActive = true)
  val rule2 = DeleteRule(DeleteRuleId(), "term2", isActive = false)

  "DeleteRule" should "create copy that has same values and a new Id" in {
    assertValues(rule1, DeleteRule.createWithNewIdFrom(rule1))
    assertValues(rule2, DeleteRule.createWithNewIdFrom(rule2))
  }

  def assertValues(expected: DeleteRule, actual: DeleteRule): Unit = {
    actual.id should not be expected.id
    actual.term shouldBe expected.term
    actual.isActive shouldBe expected.isActive
  }

}
