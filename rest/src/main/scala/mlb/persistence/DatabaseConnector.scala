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
import mlb.entities.Game
import zio.*
import zio.stream.*
import zio.jdbc.{SqlFragment, UpdateResult, ZConnectionPool, ZConnectionPoolConfig, execute, insert, selectAll, selectOne, sqlInterpolator, transaction}

import java.io.File
import java.time.LocalDate
import scala.util.Try

object DatabaseConnector {
  type Data = List[List[Option[String]]]

  def readGamesFromCSV(file: File): Seq[Game] = {
    val reader = CSVReader.open(file)
    Try {
      val rows = reader.all()
      rows.tail.flatMap { row =>
        Try {
          Game(
            GameDate(LocalDate.parse(row.head)),
            SeasonYear(row(1).toInt),
            HomeTeam(row(4)),
            AwayTeam(row(5)),
            HomeScore(row(24).toIntOption.getOrElse(-1)),
            AwayScore(row(25).toIntOption.getOrElse(-1)),
            HomeEloScore(row(6).toDouble),
            AwayEloScore(row(7).toDouble),
            HomeEloProbability(row(8).toDouble),
            AwayEloProbability(row(9).toDouble)
          )
        }.toOption
      }
    }
  }

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
            date DATE NOT NULL,
            season_year INT NOT NULL,
            home_team VARCHAR(3),
            away_team VARCHAR(3),
            home_score INT,
            away_score INT,
            home_elo DOUBLE,
            away_elo DOUBLE,
            home_prob_elo DOUBLE,
            away_prob_elo DOUBLE)
          """
    )
  }

  def insertRows(games: List[Game]): ZIO[ZConnectionPool, Throwable, UpdateResult] = {
    val rows: List[Game.Row] = games.map(_.toRow)
    transaction {
      insert(
        sql"""
             INSERT INTO games(date, season_year, home_team, away_team, home_score, away_score, home_elo, away_elo, home_prob_elo, away_prob_elo)
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

  def latestMatchOf(team: Either[HomeTeam, AwayTeam]): ZIO[ZConnectionPool, Throwable, Option[Game]] = {
    transaction {
      selectOne(
        sql"""
             SELECT * FROM games
             WHERE home_team = ${team.fold(HomeTeam.unapply, AwayTeam.unapply)}
             OR away_team = ${team.fold(HomeTeam.unapply, AwayTeam.unapply)}
             ORDER BY date
             DESC LIMIT 1
           """
          .as[Game]
      )
    }
  }

  def allMatches(team: Either[HomeTeam, AwayTeam]): ZIO[ZConnectionPool, Throwable, List[Game]] = {
    transaction {
      selectOne(
        sql"""
             SELECT * FROM games
             WHERE home_team = ${team.fold(HomeTeam.unapply, AwayTeam.unapply)}
             OR away_team = ${team.fold(HomeTeam.unapply, AwayTeam.unapply)}
             ORDER BY date
             DESC
           """
          .as[Game]
      )
    }.map(_.toList)
  }

  def predictMatch(homeTeam: HomeTeam, awayTeam: AwayTeam): ZIO[ZConnectionPool, Throwable, List[Game]] = {
    transaction {
      selectAll(
        sql"""
             SELECT * FROM games
             WHERE home_team = ${HomeTeam.unapply(homeTeam)}
             AND away_team = ${AwayTeam.unapply(awayTeam)}
             AND home_score != -1 AND away_score != -1
             ORDER BY date
             DESC
             LIMIT 20"""
          .as[Game]
      ).map(_.toList)
    }
  }
}
