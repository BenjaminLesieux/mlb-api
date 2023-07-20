# Building a ZIO Application Backend

# ZIO Application Backend
### by LESIEUX Benjamin - LIU Senhua -  MARIOTTE Thomas - PHAM Van Alenn

We coded together with JetBrains tools hosted by Benjamin.


### **Features**

We need to design and implement a Data Model.
For that, we are using Case Classes in Scala to design data models for games, teams, players and rating systems : ELO and MLB Predictions.

- `DataSet`: Dataset of the Major League Basbeball.

- `RESTful API`: The project implements a RESTful API using Scala 3 and ZIO, providing endpoints for accessing game history, making predictions, testing and retrieving relevant information from the Major League Baseball dataset. The API design focuses on usability and facilitating interaction with the dataset.

- `Data Model`: The project employs a well-designed data model that represents games, teams, players, and the two rating systems (ELO and MLB Predictions).

- `ZIO Ecosystem`: Leveraging the ZIO ecosystem, the project incorporates libraries like zio-jdbc, zio-streams, zio-json, and zio-http. This ensures the application's backend benefits from ZIO's concurrency capabilities, functional composability, and handling of database operations, streaming, JSON parsing, and HTTP interactions.

# Data Strucutre

## Read the CSV

```scala
  val app: ZIO[ZConnectionPool & Server, Throwable, Unit] = for {
    _ <- for {
      conn <- DatabaseConnector.create // Creation of the Database Connector
      data <- ZIO.fromTry(Try {
        CSVReader.open(new File("/src/mlb_elo.csv")) // Location of our dataset
      })
      games <- ZStream.fromIterator[Seq[String]](data.iterator)
        .filter(row => row.nonEmpty && row.head != "date")
        .map[Game](row => Game.fromRow(row))
        .grouped(1000)
        .foreach(g => DatabaseConnector.insertRows(g.toList))
      _ <- ZIO.succeed(data.close()) // Function for clearer dataset display
      result <- ZIO.succeed(conn)
    } yield result
    _ <- printLine("Database initialised !")
    _ <- printLine("Server is up at http://localhost:8080")
    _ <- Server.serve(static ++ mlbGamesEndpoints)
  } yield ()
 ```

▶️ We are reading the CSV thanks the ZIO library.
After creating the table, we read the .csv located at the specified path.
Processes the data using a ZStream that filters out non-empty rows and rows with a header "date," maps each row to a Game object using Game.fromRow(row), groups the games in batches of 1000, and then inserts each batch into the database using DatabaseConnector.insertRows(g.toList).
Then closes the CSV reader after processing all the data.

After the first block, there is a chain of subsequent flatMap operations that perform the following tasks:
a. Prints "Database initialised !" to the console using printLine.
b. Prints "Server is up at http://localhost:8080" to the console using printLine.
c. Starts the server at http://localhost:8080 with endpoints defined in static and mlbGamesEndpoints.

The final yield () means that the app effect returns Unit.

## Create the database

```scala
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
```
This part of code defines a database connector for interacting with a PostgreSQL database using the ZIO library in Scala. It sets up a connection pool, defines table schema, and provides methods for creating tables and inserting rows.
The important point is `createZIOPoolConfig: ULayer[ZConnectionPoolConfig]`: This value is a ZLayer that provides the configuration for the ZIO connection pool. It uses the `ZConnectionPoolConfig.default` as the configuration, which sets up a basic default configuration for the connection pool.
Then, `connectionPool` creates the actual connection pool using `ZConnectionPool.h2mem`. It connects to an in-memory H2 database named "mlb" using `properties` defined earlier.
After create the table (needed to read the .csv), we can do ZIO effect like inserting rows, find the lastest match between two teams or show predictions.

## ZIO and related libraries


We are trying to figure out how to leverage ZIO using Scala 3 to build the application backend. Hence, we can use the libraries such as zio-jdbc, zio-streams, zio-json, zio-http. The ultimate goal is to be able to parse JSON files to fetch data previously recorded along the years about the MLB.

**Screen des libraries**

`zio-jdbc`: This library provides abstractions for interacting with databases, managing connections, and executing SQL queries. By leveraging zio-jdbc, we can ensure that our database access is **safe**, composable, and takes advantage of ZIO's concurrency capabilities.

`zio-streams`: zio-streams is a library for working with streaming data in ZIO. We can use zio-streams to read and process JSON files incrementally, which is essential for parsing data without loading the entire file into memory at once.

`zio-json`: This library provides JSON encoding and decoding capabilities for ZIO applications. With zio-json, we can easily convert our data model to and from JSON format, making it convenient to interact with JSON data in our REST API endpoints and database operations.

`zio-http`: zio-http it enables us to create RESTful API endpoints that interact with the MLB dataset. We can define routes, handle HTTP requests, and respond with JSON data using zio-http's functional abstractions.


# MlbAPI.scala

## Endpoints for accessing game history and making predictions

An endpoint is a service that natively listen to requests. It is a point of entry into an SQL server, rather a way to connect to an SQL Serverinstance.
Endpoints play a crucial role in building a RESTful API. They serve as the gateways for clients to interact with the backend application and access specific functionalities. These endpoints act as the entry points to our application, allowing users to retrieve valuable data and make predictions for future games.

Our Endpoints : 

```scala
val mlbGamesEndpoints: App[ZConnectionPool] = // Creation of endpoints
    Http.collectZIO[Request] { // get a request and return a ZIO
      case request @ Method.GET -> Root / "games"  => // This endpoints is for get all games
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          games: List[Game] <- DatabaseConnector.getGames(limit)
        } yield MlbService.getGames(games)
      case request @ Method.GET -> Root / "games" / "teams" / team => // All games per teams
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        val filter = request.url.queryParams.get("filter").map(_.head)
        for {
          games: List[Game] <- DatabaseConnector.allMatches(team, limit, filter)
        } yield MlbService.getGames(games)
      case request @ Method.GET -> Root / "games" / "matchups" / team1 / "against" / team2 => // matchups between two teams
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          games: List[Game] <- DatabaseConnector.matchesBetween(team1, team2, limit)
        } yield MlbService.getMatchAgainst(games)
      case request @ Method.GET -> Root / "prediction" / "teams" / team1 / "against" / team2 => // Details between two teams
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          games: List[Game] <- DatabaseConnector.predictMatch(HomeTeam(team1), AwayTeam(team2), limit)
        } yield MlbService.predictMatch(games, HomeTeam(team1), AwayTeam(team2))
      case Method.GET -> Root / "games" / gameId => // get game ID 
        for {
          game <- DatabaseConnector.getGame(GameId(gameId.toIntOption.getOrElse(-1)))
        } yield MlbService.getGame(game)
      case request @ Method.GET -> Root / "teams" => // get all teams
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          teams <- DatabaseConnector.getTeams(limit)
        } yield MlbService.getTeams(teams)
      case request @ Method.GET -> Root / "games" / "predict" / "teams" / team1 / "against" / team2 => // Predict the winner teams between two teams
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          games: List[Game] <- DatabaseConnector.predictMatch(HomeTeam(team1), AwayTeam(team2), limit)
        } yield MlbService.predictMatch(games, HomeTeam(team1), AwayTeam(team2))
      case request @ Method.GET -> Root / "teams" / team / "pitchers" => // get details about pitchers in teams
        val limit = request.url.queryParams.get("limit").map(_.head.toInt)
        for {
          pitchers <- DatabaseConnector.getPitchers(HomeTeam(team), limit)
        } yield MlbService.getPitchers(pitchers)
      case Method.GET -> Root / "teams" / team / "eloStats" => // Get elo of teams
        for {
          eloStats <- DatabaseConnector.getEloStats(HomeTeam(team))
        } yield MlbService.getEloStats(eloStats)

    }.withDefaultErrorResponse
```

*Note: You can make the queries in the postman link we'll give you. These endpoints allow you to make the requested queries and display the results. All data comes from the dataset.*


## TEST PART

Finally, this section focuses on the functional properties of our application. We've decided to show you some screen tests of the different results we can achieve with POSTMAN and our code.

Let's see now some exemple with POSTMAN tools :

(Screen 1)

For exemple here we can see all matches.

(Screen 2)

But we can also see all matches from the same team.

So the database is link to our code and endpoints works well.
