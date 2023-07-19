package mlb

import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.defaultCSVFormat
import io.netty.handler.codec.http.QueryStringDecoder
import mlb.entities.AwayTeams.AwayTeam
import mlb.persistence.DatabaseConnector
import sun.security.provider.NativePRNG.Blocking
import zio.*
import zio.jdbc.*
import zio.http.*
import zio.stream.ZStream
import zio.Console.printLine
import mlb.entities.Game
import mlb.entities.HomeTeams.HomeTeam
import mlb.services.MlbService

import java.io.File
import java.sql.Date
import scala.util.Try

object MlbApi extends ZIOAppDefault {

  private val static: App[ZConnectionPool] =
    Http
      .collectZIO[Request] {
        case Method.GET -> Root =>
          ZIO.from(Response.json("""{"response": "API works !"}"""))
        case Method.GET -> Root / "init" =>
          ZIO.from(Response.json("""{"response": "database was initialised at startup !"}"""))
      }
      .withDefaultErrorResponse

  private val mlbGamesEndpoints: App[ZConnectionPool] =
    Http.collectZIO[Request] {
      case request @ Method.GET -> Root / "games"  =>
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          games: List[Game] <- DatabaseConnector.getGames(limit)
        } yield MlbService.getGames(games)
      case request @ Method.GET -> Root / "games" / "teams" / team =>
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        val filter = request.url.queryParams.get("filter").map(_.head)
        for {
          games: List[Game] <- DatabaseConnector.allMatches(team, limit, filter)
        } yield MlbService.getGames(games)
      case request @ Method.GET -> Root / "games" / "matchups" / team1 / "against" / team2 =>
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          games: List[Game] <- DatabaseConnector.matchesBetween(team1, team2, limit)
        } yield MlbService.getMatchAgainst(games)
      case request @ Method.GET -> Root / "prediction" / "teams" / team1 / "against" / team2 =>
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          games: List[Game] <- DatabaseConnector.predictMatch(HomeTeam(team1), AwayTeam(team2))
        } yield MlbService.predictMatch(games, HomeTeam(team1), AwayTeam(team2))
    }.withDefaultErrorResponse


  val app: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    _ <- for {
      conn <- DatabaseConnector.create
      data <- ZIO.fromTry(Try {
        CSVReader.open(new File("/Users/benjaminlesieux/Desktop/Bureau - MacBook Pro de Benjamin (4) - 1/efrei/M1/S8/Functional Programming/mlb-api/rest/src/mlb_elo.csv"))
      })
      games <- ZStream.fromIterator[Seq[String]](data.iterator)
        .filter(row => row.nonEmpty && row.head != "date")
        .map[Game](row => Game.fromRow(row))
        .grouped(1000)
        .foreach(g => DatabaseConnector.insertRows(g.toList))
      _ <- ZIO.succeed(data.close())
      result <- ZIO.succeed(conn)
    } yield result
    _ <- printLine("Database initialised !")
    _ <- printLine("Server is up at http://localhost:8080")
    _ <- Server.serve(static ++ mlbGamesEndpoints)
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] =
    app.provide(DatabaseConnector.createZIOPoolConfig >>> DatabaseConnector.connectionPool, Server.default)
}
