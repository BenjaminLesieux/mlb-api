package mlb.services

import mlb.entities.AwayTeams.AwayTeam
import zio.json.EncoderOps
import mlb.entities.Game
import mlb.entities.HomeTeams.HomeTeam
import mlb.persistence.DatabaseConnector
import zio.http.{Response, Status}

object MlbService {
  def getGames(games: List[Game]): Response = {
    games match {
      case games if games.nonEmpty => Response.json(games.toJson).withStatus(Status.Ok)
      case _ => Response.text("No games were found").withStatus(Status.NotFound)
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

        val homeEloProbability = (games.map(game => game.homeElo.toScore).sum / games.size) * 100
        val awayEloProbability = (games.map(game => game.awayElo.toScore).sum / games.size) * 100

        val homeTeamProbability = ((homeTeamWinPercentage * 0.2 + homeEloProbability) / 2) * 100
        val awayTeamProbability = ((awayTeamWinPercentage * 0.15 + awayEloProbability) / 2) * 100

        if (homeTeamProbability > awayTeamProbability) {
          Response.text(s"$homeTeamName has a $homeTeamProbability% chance of winning against $awayTeamName").withStatus(Status.Ok)
        } else {
          Response.text(s"$awayTeamName has a $awayTeamProbability% chance of winning against $homeTeamName").withStatus(Status.Ok)
        }

      case _ => Response.text("There were no encounters between those two teams").withStatus(Status.BadRequest)
    }
  }
}
