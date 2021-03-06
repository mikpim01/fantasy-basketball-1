import PlayerFormat.PlayerSerializer
import play.api.libs.json._

import scala.annotation.tailrec
import scala.io.Source
import scala.util.Random

/**
  * Created by benjaminsmith on 10/28/17.
  */

object PlayerFormat {
  case class PlayerSerializer(
                               Name:String,
                               p:Int,
                               a:Int,
                               r:Int,
                               `3`:Int,
                               s:Int,
                               b:Int,
                               to:Int,
                               fga:Int,
                               fta:Int,
                               ftm:Int,
                               fgm:Int)
  object PlayerSerializer {
    implicit val playerSerializer:Format[PlayerSerializer] = Json.format[PlayerSerializer]
  }

  def test = {
    val source: String = Source.fromFile("/Users/benjaminsmith/Programming/fantasy-basketball/src/main/resources/players_2017.json").getLines.mkString
    val json: JsValue = Json.parse(source)
    Json.fromJson[List[PlayerSerializer]](json)
  }
}

object TestData {

  val player1 = Player(points = 1, assists = 8, rebounds = 10)
  val player2 = Player(points = 2, assists = 3, rebounds = 8)
  val player3 = Player(points = 4, assists = 4, rebounds = 6)
  val player4 = Player(points = 10, assists = 2, rebounds = 1)

  val allPlayers = List(player1, player2, player3, player4)

  val agent1 = RandomAgent(Nil)
  val agent2 = MaxPointsAgent(Nil)
  val agent3 = MaxAllMonteCarloAgent(Nil, iters = 10000)

  val allAgents = List(agent3, agent2)


  def playersGenerator(n:Int):List[Player] = {
    def pair = {
      val v1 = genValue
      val v2 = genValue
      if (v1 > v2) (v1, v2)
      else (v2, v1)
    }

    def genValue = math.abs(Random.nextDouble() * 100).toInt
    val (freeThrowAttempts, freeThrowMisses) = pair
    val (fieldGoalAttempts, fieldGoalMisses) = pair
    List.fill(n){
      Player(
        points = 2,
        assists = genValue,
        rebounds = genValue,
        steals = genValue,
        blocks = genValue,
        threePointMakes = genValue,
        turnovers = genValue,
        freeThrowAttempts = freeThrowAttempts,
        freeThrowMisses = freeThrowMisses,
        fieldGoalAttempts = fieldGoalAttempts,
        fieldGoalMisses = fieldGoalMisses
      )
    }
  }

  def realPlayers:List[Player] = {
    val source: String = Source.fromFile("/Users/benjaminsmith/Programming/fantasy-basketball/src/main/resources/players_2016.json").getLines.mkString
    val json: JsValue = Json.parse(source)
    Json.fromJson[List[PlayerSerializer]](json).map{
      _.map {
        p =>
          Player(
            points = p.p,
            assists = p.a,
            rebounds = p.r,
            steals = p.s,
            blocks = p.b,
            turnovers = p.to,
            threePointMakes = p.`3`,
            freeThrowAttempts = p.fta,
            freeThrowMisses = p.ftm,
            fieldGoalAttempts = p.fga,
            fieldGoalMisses = p.fgm,
            name = p.Name
          )
      }
    } match {
      case JsError( e ) => println(e); Nil
      case JsSuccess( a, _) => a
    }
  }
}

case class Experiment(
                            startingNumberOfPlayers:List[Int],
                            numberOfRoundsInDraft:List[Int],
                            epsilons:List[Double],
                            numberOfSimulations:List[Int],
                            numberOfMonteCarloIters:List[Int],
                            numberOfCompetitors:List[Int]
                          ) {
  val allParamsSize = List(startingNumberOfPlayers.size, numberOfRoundsInDraft.size, epsilons.size, numberOfSimulations.size)

  assert(allParamsSize.forall( i => allParamsSize.headOption.contains(i)), "must have same size of list for experiment" )
}

object Experiment {
  def run(experiment: Experiment) = {

    for {
      playerCount <- experiment.startingNumberOfPlayers
      draftRounds <- experiment.numberOfRoundsInDraft
      epsilon <- experiment.epsilons
      sims <- experiment.numberOfSimulations
      iters <- experiment.numberOfMonteCarloIters
      competitorCount <- experiment.numberOfCompetitors
    } yield {
      val startTime = System.nanoTime()

      val startingPlayers = TestData.realPlayers.take(playerCount)

      val numberOfRoundsInDraft = draftRounds

      val startingEnv = Environment(startingPlayers)

      val agent1 = MaxPointsAgent(Nil)
      val agent2 = new MaxAllMonteCarloAgent(Nil, iters = iters, epsilon = epsilon)
      val allAgents = List(agent1, agent2)

      val startingAgents:List[Agent] = scala.util.Random.shuffle(allAgents)

      val scoreMap = runGameN(startingEnv,startingAgents, sims, Map(), numberOfRoundsInDraft)

      val potentialWinner = scoreMap.toList.max(Utils.tupleOrdering)

      val endTime = System.nanoTime()

      val winner = {
        if( scoreMap.toList.map(_._2).count(_ == potentialWinner._2) > 1) None
        else Some(potentialWinner)
      }

      println(
        s"""
           |Winner: ${startingAgents.find( a => winner.exists(_._1 == a.name))}
           |
         |All Agents:
           |${scoreMap.toList.map{case (agent, score) =>
          s"""
             |agent:${startingAgents.find(_.name == agent).get.name}
             |score:${score}
             |""".stripMargin
        }
        }
           |
         | RunTime = ${(endTime - startTime) / (1000 * 1000 * 1000) } seconds
           |
       """.stripMargin)
    }


    val startTime = System.nanoTime()

    val startingPlayers = TestData.realPlayers.take(40)

    val numberOfRoundsInDraft = 3

    val startingEnv = Environment(startingPlayers)

    val startingAgents:List[Agent] = scala.util.Random.shuffle(TestData.allAgents)

    val scoreMap = runGameN(startingEnv,startingAgents, 100, Map(), numberOfRoundsInDraft)

    val potentialWinner = scoreMap.toList.max(Utils.tupleOrdering)

    val endTime = System.nanoTime()

    val winner = {
      if( scoreMap.toList.map(_._2).count(_ == potentialWinner._2) > 1) None
      else Some(potentialWinner)
    }

    println(
      s"""
         |Winner: ${startingAgents.find( a => winner.exists(_._1 == a.name))}
         |
         |All Agents:
         |${scoreMap.toList.map{case (agent, score) =>
        s"""
           |agent:${startingAgents.find(_.name == agent).get.name}
           |score:${score}
           |""".stripMargin
      }
      }
         |
         | RunTime = ${(endTime - startTime) / (1000 * 1000 * 1000) } seconds
         |
       """.stripMargin)

  }

  @tailrec
  def runGameN(startingEnv:Environment, startingAgents:List[Agent], runCount:Int, scoreTracker:Map[String, Int], numberOfRoundsDraft:Int):Map[String, Int] = {
    if (runCount > 0){
      val (_, newAgents) = draft(startingEnv, startingAgents, Nil, numberOfRoundsDraft)

      val winner = MaxAllScorer.pickWinner(newAgents)

      //      println("winner", winner.map(_.name))

      val results = newAgents.map{ a =>
        if ( winner.exists(_.name == a.name) ) (a, 1)
        else (a, 0)
      }

      val newMap = results.foldLeft(scoreTracker){ case (map, (agent, score)) =>
        //        println(
        //          s"""
        //            |${"*" * 50}
        //            |agent: ${agent.name}
        //            |players:${agent.players.mkString("\n")}
        //            |${"*" * 50}
        //          """.stripMargin)
        Utils.update(map, agent.name, score)
      }

      runGameN(startingEnv, startingAgents, runCount - 1, newMap, numberOfRoundsDraft)
    }
    else scoreTracker
  }

  @tailrec
  def draft(environment: Environment,
            inAgents:List[Agent],
            outAgents:List[Agent] = Nil,
            remainingRounds:Int
           ):(Environment, List[Agent]) = {
    assert(environment.players.length >= inAgents.length * remainingRounds, "fewer players then agents * remainingRounds")
    inAgents match {
      case _ if remainingRounds <= 0 => (environment, inAgents)
      case Nil => draft(environment, outAgents, Nil, remainingRounds - 1)
      case h :: t => {
        val allOtherAgents = t ++ outAgents
        val (newEnv, newAgent) = h.action(environment, allOtherAgents)
        draft(newEnv, t, outAgents :+ newAgent, remainingRounds)
      }
    }
  }
}


object FantasyBasketball {
  def skip[A](l:List[A], n:Int) = l.zipWithIndex.collect {case (e,i) if ((i+1) % n) == 0 => e}

  def main(args: Array[String]): Unit = {
    val players = List(80)
    val iters = 1000
    val N = 1
    val epsilon = .2
    val numberOfRoundsInDraft = 3

    val startTime = System.nanoTime()

    val results = for {
      playerCount <- players
    } yield {
      println(s"doing:${playerCount} players")

      val startingPlayers = TestData.realPlayers.take(playerCount)

      val startingEnv = Environment(startingPlayers)

      val agents = List(MaxPointsAgent(Nil),MaxAllMonteCarloAgent(Nil, iters = iters, epsilon = epsilon))

      val startingAgents:List[Agent] = scala.util.Random.shuffle(agents)

      val scoreMap = Experiment.runGameN(startingEnv,startingAgents, N, Map(), numberOfRoundsInDraft)

      val gamesWonByMonteCarlo = scoreMap(MaxAllMonteCarloAgent().name)

      val fractionWon = gamesWonByMonteCarlo / N.toDouble

      (playerCount, fractionWon)
    }
    val endTime = System.nanoTime()

    println(s"number of players -> % win for ${iters} simulations, for numberOfRoundsInDraft: $numberOfRoundsInDraft, epsilon: ${epsilon}, RunTime: ${(endTime - startTime) / (1000 * 1000 * 1000) } seconds")
    results.foreach{ r =>
      println(s"${r._1}: (${math floor (r._2 * 100)}%) ${"*" * (r._2 * 100).toInt}")
    }
  }

}

case class Player(
                   points:Int = 0,
                   assists:Int = 0,
                   rebounds:Int = 0,
                   steals: Int = 0,
                   blocks:Int = 0,
                   turnovers:Int = 0,
                   threePointMakes:Int = 0,
                   freeThrowAttempts:Int = 0,
                   freeThrowMisses:Int = 0,
                   fieldGoalAttempts:Int = 0,
                   fieldGoalMisses:Int = 0,
                   name:String = "") {
  assert(freeThrowAttempts >= freeThrowMisses, s"${name}: need to attempt more free throws than makes")
  assert(fieldGoalAttempts >= fieldGoalMisses, s"${name}: need to attempt more field goals than makes")
}

case class Environment(players:List[Player]) {
  def dropPlayer(player: Player):Environment = {
    val index = players.indexOf(player)
    if (index < 0) this
    else {
      val remainingPlayers = players.patch(index, Nil, 1)
      Environment(remainingPlayers)
    }
  }
}

trait Scorer {
  def pickWinner(agents:List[Agent]):Option[Agent]
}

object MaxPointsScorer extends Scorer {
  def pickWinner(agents: List[Agent]): Option[Agent] = {
    val winner = Math.argMax(agents.map(_.players.map(_.points).sum.toDouble))
    winner.map(agents(_))
  }
}

object MaxAllScorer extends Scorer {
  def pickWinner(agents: List[Agent]): Option[Agent] = {
    val pointsWinner = Math.argMax(agents.map(_.players.map(_.points).sum.toDouble))
    val assistsWinner = Math.argMax(agents.map(_.players.map(_.assists).sum.toDouble))
    val reboundsWinner = Math.argMax(agents.map(_.players.map(_.rebounds).sum.toDouble))
    val stealsWinner = Math.argMax(agents.map(_.players.map(_.steals).sum.toDouble))
    val blocksWinner = Math.argMax(agents.map(_.players.map(_.blocks).sum.toDouble))
    val turnoverWinner = Math.argMin(agents.map(_.players.map(_.turnovers).sum.toDouble))
    val threePointMakesWinner = Math.argMax(agents.map(_.players.map(_.threePointMakes).sum.toDouble))
    val freeThrowAttemptsWinner = Math.argMax(agents.map(_.players.map(_.freeThrowAttempts).sum.toDouble))
    val fieldGoalPercentageWinner = Math.argMax(agents.map{ a =>
      val attempts = a.players.map(_.fieldGoalAttempts).sum
      val misses = a.players.map(_.fieldGoalMisses).sum
      if (attempts == 0) 0.0
      else 1.0 - (misses.toDouble / attempts)
    })
    val fieldThrowPercentageWinner = Math.argMax(agents.map{ a =>
      val attempts = a.players.map(_.freeThrowAttempts).sum
      val misses = a.players.map(_.freeThrowMisses).sum
      if (attempts == 0) 0.0
      else 1.0 - (misses.toDouble / attempts)
    })

    //TODO: Add the percent categories
    val scoreGroup = List(
      pointsWinner,
      assistsWinner,
      reboundsWinner,
      stealsWinner,
      blocksWinner,
      turnoverWinner,
      threePointMakesWinner,
      freeThrowAttemptsWinner,
      fieldGoalPercentageWinner,
      fieldThrowPercentageWinner
    ).flatten.groupBy(identity)

    val (potentialWinner, wins) = scoreGroup.maxBy(_._2.size)
    //If the winner has the same number of wins as someone else, nobody wins
      val overallWinner = {
        if( scoreGroup.toList.map(_._2.size).count(_ == wins.size) > 1) None
        else Some(potentialWinner)
      }

    overallWinner.map(agents(_))
  }
}

object Utils {
  def update[A](score:Map[A, Int], player:A, value:Int):Map[A, Int] =
    score + (player -> score.get(player).map(_ + value).getOrElse(value))

  def updateCumulativeAverageReward[A](score:Map[A, (Double, Int)], player:A, value:Int):Map[A, (Double, Int)] =
    score + (player -> score.get(player).map{ case (cumAverageReward, count) => (((cumAverageReward * count ) + value) / (count + 1), count + 1)}.getOrElse((value.toDouble, 1)))

  def tupleOrdering = new Ordering[(_, Int)] {
    override def compare(x: (_, Int), y: (_, Int)): Int = {
      if(x._2 > y._2) 1
      else if (x._2 < y._2) -1
      else 0
    }
  }
  def tupleDoubleOrdering = new Ordering[(_, Double)] {
    override def compare(x: (_, Double), y: (_, Double)): Int = {
      if(x._2 > y._2) 1
      else if (x._2 < y._2) -1
      else 0
    }
  }
}