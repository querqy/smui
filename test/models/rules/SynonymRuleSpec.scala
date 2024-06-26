package models.rules

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SynonymRuleSpec extends AnyFlatSpec with Matchers {

  val rule1 = SynonymRule(SynonymRuleId(), SynonymRule.TYPE_DIRECTED, "term1", isActive = true)
  val rule2 = SynonymRule(SynonymRuleId(), SynonymRule.TYPE_UNDIRECTED, "term2", isActive = false)

  "SynonymRule" should "create copy that has same values and a new Id" in {
    assertValues(rule1, SynonymRule.createWithNewIdFrom(rule1))
    assertValues(rule2, SynonymRule.createWithNewIdFrom(rule2))
  }

  def assertValues(expected: SynonymRule, actual: SynonymRule): Unit = {
    actual.id should not be expected.id
    actual.term shouldBe expected.term
    actual.synonymType shouldBe expected.synonymType
    actual.isActive shouldBe expected.isActive
  }

}
