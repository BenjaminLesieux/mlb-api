package mlb.entities

import mlb.entities.GameIds.GameId
import mlb.entities.HomeEloScores.HomeEloScore
import mlb.entities.Pitchers.Pitcher
import zio.json.*
import zio.jdbc.*

import java.time.LocalDate
import scala.annotation.targetName

object GameIds {
  opaque type GameId = Int
  object GameId {
    def apply(value: Int): GameId = value
    def unapply(gameId: GameId): Int = gameId
  }

  given CanEqual[GameId, GameId] = CanEqual.derived

  implicit val gameIdEncoder: JsonEncoder[GameId] = JsonEncoder.int
  implicit val gameIdDecoder: JsonDecoder[GameId] = JsonDecoder.int
}

object Pitchers {
  opaque type Pitcher = String
  object Pitcher {
      def apply(value: String): Pitcher = value
      def unapply(pitcher: Pitcher): String = pitcher match
        case "" => "TBD"
        case _ => pitcher
  }

  given CanEqual[Pitcher, Pitcher] = CanEqual.derived

  implicit val pitcherEncoder: JsonEncoder[Pitcher] = JsonEncoder.string
  implicit val pitcherDecoder: JsonDecoder[Pitcher] = JsonDecoder.string

  implicit val pitcherJdbcDecoder: JdbcDecoder[Pitcher] = JdbcDecoder.stringDecoder
}

object HomeTeams {
  opaque type HomeTeam = String
  object HomeTeam {
    def apply(value: String): HomeTeam = value
    def unapply(homeTeam: HomeTeam): String = homeTeam
  }

  given CanEqual[HomeTeam, HomeTeam] = CanEqual.derived

  implicit val homeTeamEncoder: JsonEncoder[HomeTeam] = JsonEncoder.string
  implicit val homeTeamDecoder: JsonDecoder[HomeTeam] = JsonDecoder.string

  implicit val homeTeamJdbcDecoder: JdbcDecoder[HomeTeam] = JdbcDecoder.stringDecoder

  extension (homeTeam: HomeTeam) {
    def toTeam: String = homeTeam
  }
}

object AwayTeams {
  opaque type AwayTeam = String

  object AwayTeam {

    def apply(value: String): AwayTeam = value

    def unapply(awayTeam: AwayTeam): String = awayTeam
  }

  given CanEqual[AwayTeam, AwayTeam] = CanEqual.derived
  implicit val awayTeamEncoder: JsonEncoder[AwayTeam] = JsonEncoder.string
  implicit val awayTeamDecoder: JsonDecoder[AwayTeam] = JsonDecoder.string

  extension (awayTeam: AwayTeam) {
    def toTeam: String = awayTeam
  }
}

object HomeScores {

  opaque type HomeScore = Int
  object HomeScore {

    def apply(value: Int): HomeScore = value

    def unapply(homeScore: HomeScore): Int = homeScore
  }

  given CanEqual[HomeScore, HomeScore] = CanEqual.derived
  implicit val homeScoreEncoder: JsonEncoder[HomeScore] = JsonEncoder.int
  implicit val homeScoreDecoder: JsonDecoder[HomeScore] = JsonDecoder.int

  extension (homeScore: HomeScore) {
    def toScore: Int = homeScore
  }
}

object AwayScores {

  opaque type AwayScore = Int

  object AwayScore {

    def apply(value: Int): AwayScore = value

    def unapply(awayScore: AwayScore): Int = awayScore
  }

  given CanEqual[AwayScore, AwayScore] = CanEqual.derived
  implicit val awayScoreEncoder: JsonEncoder[AwayScore] = JsonEncoder.int
  implicit val awayScoreDecoder: JsonDecoder[AwayScore] = JsonDecoder.int

  extension (awayScore: AwayScore) {
    def toScore: Int = awayScore
  }
}

object HomeEloScores {

  opaque type HomeEloScore = Double

  object HomeEloScore {
    def apply(value: Double): HomeEloScore = value
    def unapply(eloScore: HomeEloScore): Double = eloScore
  }

  given CanEqual[HomeEloScore, HomeEloScore] = CanEqual.derived
  implicit val eloScoreEncoder: JsonEncoder[HomeEloScore] = JsonEncoder.double
  implicit val eloScoreDecoder: JsonDecoder[HomeEloScore] = JsonDecoder.double

  extension (eloScore: HomeEloScore) {
    def toScore: Double = eloScore
  }
}

object AwayEloScores {

  opaque type AwayEloScore = Double

  object AwayEloScore {
    def apply(value: Double): AwayEloScore = value
    def unapply(eloScore: AwayEloScore): Double = eloScore

    extension (eloScore: AwayEloScore) {
      def toScore: Double = eloScore
    }
  }

  given CanEqual[AwayEloScore, AwayEloScore] = CanEqual.derived
  implicit val awayEloEncoder: JsonEncoder[AwayEloScore] = JsonEncoder.double
  implicit val awayEloDecoder: JsonDecoder[AwayEloScore] = JsonDecoder.double
}

object HomeEloProbabilities {

  opaque type HomeEloProbability = Double

  object HomeEloProbability {
    def apply(value: Double): HomeEloProbability = value
    def unapply(eloProbabilityScore: HomeEloProbability): Double =
      eloProbabilityScore
  }

  given CanEqual[HomeEloProbability, HomeEloProbability] = CanEqual.derived
  implicit val homeEloProbabilityEncoder: JsonEncoder[HomeEloProbability] = JsonEncoder.double
  implicit val homeEloProbabilityDecoder: JsonDecoder[HomeEloProbability] = JsonDecoder.double

  extension(homeEloProbability: HomeEloProbability) {
    def toScore: Double = homeEloProbability
  }
}

object AwayEloProbabilities {

  opaque type AwayEloProbability = Double

  object AwayEloProbability {
    def apply(value: Double): AwayEloProbability = value
    def unapply(eloProbabilityScore: AwayEloProbability): Double =
      eloProbabilityScore
  }

  given CanEqual[AwayEloProbability, AwayEloProbability] = CanEqual.derived
  implicit val awayEloProbabilityEncoder: JsonEncoder[AwayEloProbability] = JsonEncoder.double
  implicit val awayEloProbabilityDecoder: JsonDecoder[AwayEloProbability] = JsonDecoder.double

  extension(awayEloProbability: AwayEloProbability) {
    def toScore: Double = awayEloProbability
  }
}

object GameDates {

  opaque type GameDate = LocalDate

  object GameDate {

    def apply(value: LocalDate): GameDate = value
    def unapply(gameDate: GameDate): LocalDate = gameDate
  }

  given CanEqual[GameDate, GameDate] = CanEqual.derived
  implicit val gameDateEncoder: JsonEncoder[GameDate] = JsonEncoder.localDate
  implicit val gameDateDecoder: JsonDecoder[GameDate] = JsonDecoder.localDate
}

object SeasonYears {

  opaque type SeasonYear <: Int = Int

  object SeasonYear {

    def apply(year: Int): SeasonYear = year

    def safe(value: Int): Option[SeasonYear] =
      Option.when(value >= 1876 && value <= LocalDate.now.getYear)(value)

    def unapply(seasonYear: SeasonYear): Int = seasonYear
  }

  given CanEqual[SeasonYear, SeasonYear] = CanEqual.derived
  implicit val seasonYearEncoder: JsonEncoder[SeasonYear] = JsonEncoder.int
  implicit val seasonYearDecoder: JsonDecoder[SeasonYear] = JsonDecoder.int
}

import GameDates.*
import SeasonYears.*
import HomeTeams.*
import AwayTeams.*
import HomeScores.*
import AwayScores.*
import HomeEloScores.*
import AwayEloScores.*
import HomeEloProbabilities.*
import AwayEloProbabilities.*

// Creating Game class
final case class Game(
    id: GameId,
    date: GameDate,
    season: SeasonYear,
    homeTeam: HomeTeam,
    awayTeam: AwayTeam,
    homeScore: HomeScore,
    awayScore: AwayScore,
    homeElo: HomeEloScore,
    awayElo: AwayEloScore,
    homeProbElo: HomeEloProbability,
    awayProbElo: AwayEloProbability,
    homePitcher: Pitcher,
    awayPitcher: Pitcher
)

object Game {

  given CanEqual[Game, Game] = CanEqual.derived
  implicit val gameEncoder: JsonEncoder[Game] = DeriveJsonEncoder.gen[Game]
  implicit val gameDecoder: JsonDecoder[Game] = DeriveJsonDecoder.gen[Game]

  def unapply(
      game: Game
  ): (
      GameId,
      GameDate,
      SeasonYear,
      HomeTeam,
      AwayTeam,
      HomeScore,
      AwayScore,
      HomeEloScore,
      AwayEloScore,
      HomeEloProbability,
      AwayEloProbability,
      Pitcher,
      Pitcher
  ) =
    (
      game.id,
      game.date,
      game.season,
      game.homeTeam,
      game.awayTeam,
      game.homeScore,
      game.awayScore,
      game.homeElo,
      game.awayElo,
      game.homeProbElo,
      game.awayProbElo,
      game.homePitcher,
      game.awayPitcher
    )

  // a custom decoder from a tuple
  type Row =
    (Int, String, Int, String, String, Int, Int, Double, Double, Double, Double, String, String)

  def fromRow(filteredRow: Seq[String], idx: Int = 0): Game = {
    Game(
      GameId(idx),
      GameDate(LocalDate.parse(filteredRow.head)),
      SeasonYear(filteredRow(1).toInt),
      HomeTeam(filteredRow(4)),
      AwayTeam(filteredRow(5)),
      HomeScore(filteredRow(24).toIntOption.getOrElse(-1)),
      AwayScore(filteredRow(25).toIntOption.getOrElse(-1)),
      HomeEloScore(filteredRow(6).toDouble),
      AwayEloScore(filteredRow(7).toDouble),
      HomeEloProbability(filteredRow(8).toDouble),
      AwayEloProbability(filteredRow(9).toDouble),
      Pitcher(filteredRow(14)),
      Pitcher(filteredRow(15))
    )
  }

  extension (g: Game)
    def toRow: Row =
      val (id, d, y, h, a, hs, as, he, ae, hpe, ape, hPitch, aPitch) = Game.unapply(g)
      (
        GameId.unapply(id),
        GameDate.unapply(d).toString,
        SeasonYear.unapply(y),
        HomeTeam.unapply(h),
        AwayTeam.unapply(a),
        HomeScore.unapply(hs),
        AwayScore.unapply(as),
        HomeEloScore.unapply(he),
        AwayEloScore.unapply(ae),
        HomeEloProbability.unapply(hpe),
        AwayEloProbability.unapply(ape),
        Pitcher.unapply(hPitch),
        Pitcher.unapply(aPitch)
      )

  implicit val jdbcDecoder: JdbcDecoder[Game] = JdbcDecoder[Row]().map[Game] {
    t =>
      val (
        id,
        date,
        season,
        home,
        away,
        homeScore,
        awayScore,
        homeElo,
        awayElo,
        homeProbElo,
        awayProbElo,
        homePitcher,
        awayPitcher
      ) = t
      Game(
        GameId(id),
        GameDate(LocalDate.parse(date)),
        SeasonYear(season),
        HomeTeam(home),
        AwayTeam(away),
        HomeScore(homeScore),
        AwayScore(awayScore),
        HomeEloScore(homeElo),
        AwayEloScore(awayElo),
        HomeEloProbability(homeProbElo),
        AwayEloProbability(awayProbElo),
        Pitcher(homePitcher),
        Pitcher(awayPitcher)
      )
  }
}

case class EloStats(team: HomeTeam, elo_score: HomeEloScore, elo_prob: AwayEloScore)

object EloStats {
  implicit val decoder: JdbcDecoder[EloStats] = JdbcDecoder[(String, Double, Double)]().map[EloStats] { e =>
    val (team, elo_score, elo_prob) = e
    EloStats(HomeTeam(team), HomeEloScore(elo_score), AwayEloScore(elo_prob))
  }
  implicit val jsonEncoder: JsonEncoder[EloStats] = DeriveJsonEncoder.gen[EloStats]
  implicit val jsonDecoder: JsonDecoder[EloStats] = DeriveJsonDecoder.gen[EloStats]
}
