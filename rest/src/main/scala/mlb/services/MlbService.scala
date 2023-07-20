package mlb.services

import mlb.entities.AwayTeams.AwayTeam
import zio.json.EncoderOps
import mlb.entities.{EloStats, Game}
import mlb.entities.HomeTeams.HomeTeam
import mlb.entities.Pitchers.Pitcher
import mlb.persistence.DatabaseConnector
import zio.http.{Response, Status}

object MlbService {
  def getGames(games: List[Game]): Response = {
    games match {
      case games if games.nonEmpty => Response.json(games.toJson).withStatus(Status.Ok)
      case _ => Response.text("No games were found").withStatus(Status.NotFound)
    }
  }

  def getGame(game: Option[Game]): Response = {
    game match {
      case Some(game) => Response.json(game.toJson).withStatus(Status.Ok)
      case None => Response.text("No game was found").withStatus(Status.NotFound)
    }
  }

  def getTeams(teams: List[HomeTeam]): Response = {
    teams match {
      case teams if teams.nonEmpty => Response.json(teams.toJson).withStatus(Status.Ok)
      case _ => Response.text("No teams were found").withStatus(Status.NotFound)
    }
  }

  def getEloStats(stats: List[EloStats]): Response = {
    stats match {
      case stats if stats.nonEmpty =>
        val meanScores = stats.map(stat => stat.elo_score.toScore).sum / stats.size
        val meanProbabilities = stats.map(stat => stat.elo_prob.toScore).sum / stats.size
        Response.text(s"Mean elo score: $meanScores, Mean elo probability: $meanProbabilities").withStatus(Status.Ok)
      case _ => Response.text("No elo stats were found").withStatus(Status.NotFound)
    }
  }

  def getPitchers(pitchers: List[Pitcher]): Response = {
    pitchers match {
      case pitchers if pitchers.nonEmpty => Response.json(pitchers.toJson).withStatus(Status.Ok)
      case _ => Response.text("No pitchers were found").withStatus(Status.NotFound)
    }
  }

  def getMatchAgainst(games: List[Game]): Response = {
    games match {
      case games if games.nonEmpty => Response.json(games.toJson).withStatus(Status.Ok)
      case _ => Response.text("There were no encounters between those two teams").withStatus(Status.BadRequest)
    }
  }

  def predictMatch(games: List[Game], team1: HomeTeam, team2: AwayTeam): Response = {
    games match {
      case games if games.nonEmpty =>
        val homeTeamName = team1.toTeam
        val awayTeamName = team2.toTeam

        val homeTeamWins = games.count(game => game.homeScore.toScore > game.awayScore.toScore)
        val awayTeamWins = games.count(game => game.awayScore.toScore > game.homeScore.toScore)

        val homeTeamWinPercentage = (homeTeamWins / games.size) * 100
        val awayTeamWinPercentage = (awayTeamWins / games.size) * 100

        val homeEloProbability = (games.map(game => game.homeProbElo.toScore).sum / games.size) * 100
        val awayEloProbability = (games.map(game => game.awayProbElo.toScore).sum / games.size) * 100

        val homeEloString = f"$homeEloProbability%1.2f"
        val awayEloString = f"$awayEloProbability%1.2f"

        if (homeEloProbability > awayEloProbability)
          Response.text(s"$homeTeamName has a $homeEloString% chance of winning against $awayTeamName").withStatus(Status.Ok)
        else
          Response.text(s"$awayTeamName has a $awayEloString% chance of winning against $homeTeamName").withStatus(Status.Ok)

      case _ => Response.text("There were no encounters between those two teams").withStatus(Status.BadRequest)
    }
  }
}
