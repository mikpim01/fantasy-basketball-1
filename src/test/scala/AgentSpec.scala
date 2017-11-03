import org.scalatest.{FlatSpec, FunSuite}

/**
  * Created by benjaminsmith on 10/29/17.
  */
class AgentSpec extends FlatSpec {
  behavior of "Max Points Agent"

  it should "pick the player with the max points and update the env" in {
    val maxPointsAgent = MaxPointsAgent()

    val p1 = Player(10)
    val p2 = Player(12)

    val environment = Environment(List(p1,p2))

    val (newEnv, newAgent) = maxPointsAgent.action(environment, Nil)

    assert(newAgent.players == List(p2))
    assert(newEnv.players == List(p1))
  }

  behavior of "Monte carlo agent"

  it should "update func should increment score for already existing player" in {
    val testPlayer = Player(1)
    val testMap = Map(testPlayer -> 1)
    val newMap = Utils.update(testMap, testPlayer, 1)
    assert(newMap == Map(testPlayer -> 2))
  }

  it should "make a new entry for a player" in {
    val testPlayer = Player(1)
    val testMap:Map[Player, Int] = Map()
    val newMap = Utils.update(testMap, testPlayer, 1)
    assert(newMap == Map(testPlayer -> 1))
  }

  it should "draft random" in {
    val p1 = Player(2)
    val p2 = Player(10)

    val environment = Environment(players = List(p1, p2))

    val monteAgent = Utils()

    val competitor = RandomAgent()

    val (newEnv, newAgent) = monteAgent.action(environment, List(competitor))

    //This should almost always happen
    assert(newAgent.players == List(p2))

  }
}