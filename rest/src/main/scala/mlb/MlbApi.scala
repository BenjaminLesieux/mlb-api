package mlb

import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.defaultCSVFormat
import mlb.persistence.DatabaseConnector
import sun.security.provider.NativePRNG.Blocking
import zio.*
import zio.Console.printLine
import zio.jdbc.*
import zio.http.*
import mlb.entities.Game

import java.io.File
import java.sql.Date

object MlbApi extends ZIOAppDefault {

  val endpoints: App[ZConnectionPool] =
    Http
      .collectZIO[Request] {
        case Method.GET -> Root => ZIO.from(Response.json("""{"response": "API works !"}"""))
        case Method.GET -> Root / "init" => {
          ZIO.from(Response.json("""{"response": "database initialised !"}"""))
        }
        case Method.GET -> Root / "games" => ???

        case Method.GET -> Root / "predict" / "game" / gameId => ???
      }
      .withDefaultErrorResponse

  val app: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    conn <- DatabaseConnector.create
    games <- DatabaseConnector.readGamesFromCSV("./data/mlb_elo.csv")
    _ <- Console.printLine("Data Loaded")
    _ <- Server.serve(endpoints)
  } yield ()

  override def run: ZIO[Any, Throwable, Unit] =
    app.provide(DatabaseConnector.createZIOPoolConfig >>> DatabaseConnector.connectionPool, Server.default)
}
