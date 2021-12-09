package models

import anorm.SqlParser.get
import anorm._
import play.api.libs.json._

import java.sql.Connection
import java.time.LocalDateTime

class TeamId(id: String) extends Id(id)
object TeamId extends IdObject[TeamId](new TeamId(_))

/**
  * Defines a team
  */
case class Team(id: TeamId = TeamId(),
                    name: String,
                    lastUpdate: LocalDateTime = LocalDateTime.now()) {

  import Team._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    NAME -> name,
    LAST_UPDATE -> lastUpdate
  )

  def displayValue: String = name

}

object Team {

  val TABLE_NAME = "team"
  val TABLE_NAME_TEAM_2_SOLR_INDEX = "team_2_solr_index"

  val ID = "id"
  val NAME = "name"
  val LAST_UPDATE = "last_update"

  val TEAM_ID = "team_id"
  val SOLR_INDEX_ID = "solr_index_id"

  implicit val jsonReads: Reads[Team] = Json.reads[Team]

  private val defaultWrites: OWrites[Team] = Json.writes[Team]
  implicit val jsonWrites: OWrites[Team] = OWrites[Team] { team =>
    defaultWrites.writes(team)
  }

  def create(name: String): Team = {
    Team(TeamId(), name, LocalDateTime.now())
  }

  val sqlParser: RowParser[Team] = get[TeamId](s"$TABLE_NAME.$ID") ~
    get[String](s"$TABLE_NAME.$NAME") ~
    get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ name ~ lastUpdate =>
    Team(id, name, lastUpdate)
  }

  def getTeam(teamId: String)(implicit connection: Connection): Team = {
    SQL"select * from #$TABLE_NAME where id = $teamId order by #$NAME asc"
      .as(sqlParser.*).head
  }

  def insert(newTeams: Team*)(implicit connection: Connection): Unit = {
    if (newTeams.nonEmpty) {
      BatchSql(s"insert into $TABLE_NAME ($ID, $NAME, $LAST_UPDATE) " +
        s"values ({$ID}, {$NAME}, {$LAST_UPDATE})",
        newTeams.head.toNamedParameters,
        newTeams.tail.map(_.toNamedParameters): _*
      ).execute()
    }
  }

  def update(id: TeamId, name: String)(implicit connection: Connection): Int = {
    SQL"update #$TABLE_NAME set #$NAME = $name, #$LAST_UPDATE = ${LocalDateTime.now()} where #$ID = $id".executeUpdate()
  }

  def deleteByIds(ids: Seq[TeamId])(implicit connection: Connection):Int = {
    var count = 0
    for (idGroup <- ids.grouped(100)) {
      count += SQL"delete from #$TABLE_NAME where #$ID in ($idGroup)".executeUpdate()
    }
    count
  }

  def loadAll()(implicit connection: Connection): Seq[Team] = {
    SQL"select * from #$TABLE_NAME order by #$NAME asc"
      .as(sqlParser.*)
  }

  def getTeam2SolrIndex(selectId: String, isLeftToRight: Boolean)(implicit connection: Connection): List[String] = {
    val selectFieldName = if (isLeftToRight) TEAM_ID else SOLR_INDEX_ID
    val returnFieldName = if (isLeftToRight) SOLR_INDEX_ID else TEAM_ID
    SQL"select #$returnFieldName from #$TABLE_NAME_TEAM_2_SOLR_INDEX where #$selectFieldName=$selectId order by #$returnFieldName asc"
      .as(SqlParser.str(0).*)
  }


}
