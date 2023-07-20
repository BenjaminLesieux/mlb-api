package mlb.persistence

import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.defaultCSVFormat
import mlb.entities.GameDates.GameDate
import mlb.entities.HomeEloScores.HomeEloScore
import mlb.entities.HomeEloProbabilities.HomeEloProbability
import mlb.entities.HomeScores.HomeScore
import mlb.entities.SeasonYears.SeasonYear
import mlb.entities.AwayEloScores.AwayEloScore
import mlb.entities.AwayEloProbabilities.AwayEloProbability
import mlb.entities.AwayScores.AwayScore
import mlb.entities.HomeTeams.HomeTeam
import mlb.entities.AwayTeams.AwayTeam
import mlb.entities.{EloStats, Game}
import mlb.entities.GameIds.GameId
import mlb.entities.Pitchers.Pitcher
import zio.*
import zio.stream.*
import zio.jdbc.{JdbcDecoder, SqlFragment, UpdateResult, ZConnectionPool, ZConnectionPoolConfig, execute, insert, selectAll, selectOne, sqlInterpolator, transaction}
import zio.json.JsonEncoder

import java.io.File
import java.time.LocalDate
import scala.util.Try

object DatabaseConnector {
  type Data = List[List[Option[String]]]

  val createZIOPoolConfig: ULayer[ZConnectionPoolConfig] =
    ZLayer.succeed(ZConnectionPoolConfig.default)

  private val properties: Map[String, String] = Map(
    "user" -> "postgres",
    "password" -> "postgres"
  )

  val connectionPool
      : ZLayer[ZConnectionPoolConfig, Throwable, ZConnectionPool] =
    ZConnectionPool.h2mem(
      database = "mlb",
      props = properties
    )

  val create: ZIO[ZConnectionPool, Throwable, Unit] = transaction {
    execute(
      sql"""
           CREATE TABLE IF NOT EXISTS games(
            id SERIAL PRIMARY KEY,
            date DATE NOT NULL,
            season_year INT NOT NULL,
            home_team VARCHAR(3),
            away_team VARCHAR(3),
            home_score INT,
            away_score INT,
            home_elo DOUBLE,
            away_elo DOUBLE,
            home_prob_elo DOUBLE,
            away_prob_elo DOUBLE,
            home_pitcher VARCHAR(50),
            away_pitcher VARCHAR(50)
            )
          """
    )
  }

  def insertRows(games: List[Game]): ZIO[ZConnectionPool, Throwable, UpdateResult] = {
    val rows: List[Game.Row] = games.map(_.toRow)
    transaction {
      insert(
        sql"""
             INSERT INTO games(id, date, season_year, home_team, away_team, home_score, away_score, home_elo, away_elo, home_prob_elo, away_prob_elo, home_pitcher, away_pitcher)
           """
          .values[Game.Row](rows)
      )
    }
  }

  val count: ZIO[ZConnectionPool, Throwable, Option[Int]] = transaction {
    selectOne(
      sql"SELECT COUNT(*) FROM games".as[Int]
    )
  }

  def latestMatchBetween(homeTeam: HomeTeam, awayTeam: AwayTeam): ZIO[ZConnectionPool, Throwable, Option[Game]] = {
    transaction {
      selectOne(
        sql"""
             SELECT * FROM games
             WHERE home_team = ${HomeTeam.unapply(homeTeam)} AND away_team = ${AwayTeam.unapply(awayTeam)}
             ORDER BY date
             DESC LIMIT 1
           """
          .as[Game]
      )
    }
  }

  def matchesBetween(team1: String, team2: String, limit: Option[Int]): ZIO[ZConnectionPool, Throwable, List[Game]] = {
    transaction {
      selectAll(
        sql"""
                   SELECT * FROM games
                   WHERE (home_team = $team1 AND away_team = $team2) OR (home_team = $team2 AND away_team = $team1)
                   ORDER BY date
                   DESC LIMIT ${limit.getOrElse(20)}
                 """
          .as[Game]
      ).map(_.toList)
    }
  }

  def predictMatch(team1: HomeTeam, team2: AwayTeam, limit: Option[Int]): ZIO[ZConnectionPool, Throwable, List[Game]] = {
    transaction {
      selectAll(
        sql"""
                   SELECT * FROM games
                   WHERE home_team = ${HomeTeam.unapply(team1)} AND away_team = ${AwayTeam.unapply(team2)}
                   AND home_score != -1 AND away_score != -1
                   ORDER BY date
                   DESC LIMIT ${limit.getOrElse(20)}
                 """
          .as[Game]
      ).map(_.toList)
    }
  }

  def allMatches(team: String, limit: Option[Int], filter: Option[String]): ZIO[ZConnectionPool, Throwable, List[Game]] = {
    filter.getOrElse("all").toLowerCase() match {
      case "away" => transaction {
        selectAll(
          sql"""
               SELECT * FROM games
               WHERE away_team = $team
               ORDER BY date
               DESC
               LIMIT ${limit.getOrElse(20)}"""
            .as[Game]
        ).map(_.toList)
      }
      case "home" => transaction {
          selectAll(
          sql"""
               SELECT * FROM games
               WHERE home_team = $team
               ORDER BY date
               DESC
               LIMIT ${limit.getOrElse(20)}"""
              .as[Game]
          ).map(_.toList)
      }
      case _ => transaction {
        selectAll(
          sql"""
               SELECT * FROM games
               WHERE home_team = $team OR away_team = $team
               ORDER BY date
               DESC
               LIMIT ${limit.getOrElse(20)}"""
            .as[Game]
        ).map(_.toList)
      }
    }
  }

  def getGame(id: GameId): ZIO[ZConnectionPool, Throwable, Option[Game]] = {
    transaction {
      selectOne(
        sql"""
             SELECT * FROM games
             WHERE id = ${GameId.unapply(id)}"""
          .as[Game]
      )
    }
  }

  def getGames(limit: Option[Int]): ZIO[ZConnectionPool, Throwable, List[Game]] = {
    transaction {
      selectAll(
        sql"""
             SELECT * FROM games
             WHERE home_score != -1 AND away_score != -1
             ORDER BY date
             DESC
             LIMIT ${limit.getOrElse(20)}"""
          .as[Game]
      ).map(_.toList)
    }
  }

  def getTeams(limit: Option[Int]): ZIO[ZConnectionPool, Throwable, List[HomeTeam]] = {
    transaction {
      selectAll(
        sql"""
             SELECT DISTINCT home_team FROM games
             ORDER BY home_team
             LIMIT ${limit.getOrElse(20)}"""
          .as[HomeTeam]
      ).map(_.toList)
    }
  }

  def getPitchers(team: HomeTeam, limit: Option[Int]): ZIO[ZConnectionPool, Throwable, List[Pitcher]] = {
    transaction {
      selectAll(
        sql"""
             SELECT DISTINCT * FROM (
                SELECT home_pitcher FROM games
                WHERE home_team = ${HomeTeam.unapply(team)} and home_pitcher != 'TBD'
                UNION (
                        SELECT away_pitcher FROM games
                        WHERE away_team = ${HomeTeam.unapply(team)} and away_pitcher != 'TBD'
                        )
             ) as pitchers_at_home
             """
          .as[Pitcher]
      ).map(_.toList)
    }
  }

  def getEloStats(team: HomeTeam): ZIO[ZConnectionPool, Throwable, List[EloStats]] = {
    transaction {
      selectAll(
        sql"""
             SELECT team, elo_score, elo_prob FROM (
                SELECT home_team as team, home_elo as elo_score, home_prob_elo as elo_prob FROM games
                WHERE home_team = ${HomeTeam.unapply(team)}
                UNION (
                        SELECT away_team as team, away_elo as elo_score, away_prob_elo as elo_prob FROM games
                        WHERE away_team = ${HomeTeam.unapply(team)}
                        )
             ) as elo_stats
             """
          .as[EloStats]
      ).map(_.toList)
    }
  }
}
