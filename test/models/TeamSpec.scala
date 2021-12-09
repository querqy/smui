package models

import org.h2.jdbc.JdbcBatchUpdateException
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import utils.WithInMemoryDB

class TeamSpec extends FlatSpec with Matchers with BeforeAndAfterEach with WithInMemoryDB {

  private val teams = Seq(
    Team.create("team1"),
    Team.create("team2"),
    Team.create("team3"),
    Team.create("team4")
  )

  // Accuracy of lastUpdate before/after database insert should omit nano seconds
  def adjustedTimeAccuracy(team: Seq[Team]): Seq[Team] = teams.map(team =>
    team.copy(lastUpdate = team.lastUpdate.withNano(0))
  )

  "Team" should "be saved to the database and read out again" in {
    db.withConnection { implicit connection =>
      Team.insert(teams: _*)
      val loaded = Team.loadAll()
      adjustedTimeAccuracy(loaded).toSet shouldBe adjustedTimeAccuracy(teams).toSet
    }

  }

  it should "not allow inserting the same name more than once" in {
    val team = Team.create("name10")
    db.withConnection { implicit connection =>
      intercept[JdbcBatchUpdateException] {
        Team.insert(team, team)
      }
    }
  }

}
