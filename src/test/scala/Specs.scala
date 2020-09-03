import zio.duration._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.environment._

object Specs extends DefaultRunnableSpec {
  def spec: ZSpec[TestEnvironment, Any] =
    suite("Tests")(
      testM("Test the game") {
        for {
          _             <- TestRandom.feedInts(3)
          _             <- TestConsole.feedLines("3")
          game          <- Main1.program2
          consoleOutput <- TestConsole.output
        } yield {
          assert(game)(equalTo(true)) && assert(consoleOutput)(
            equalTo(Vector("Please enter a random number between 0 and 10:\n", "You won\n")))
        }
      },
      testM("Test the game with retries 1") {
        for {
          _             <- TestRandom.feedInts(3, 3, 3)
          _             <- TestConsole.feedLines("a", "a", "3")
          game          <- Main1.program3.fork
          _             <- TestClock.adjust(4.seconds)
          result        <- game.join
          consoleOutput <- TestConsole.output

        } yield {
          assert(result)(equalTo(true)) &&
          assert(consoleOutput)(equalTo(Vector(
            "Please enter a random number between 0 and 10:\n",
            "Please enter a random number between 0 and 10:\n",
            "Please enter a random number between 0 and 10:\n",
            "You won\n"
          )))
        }
      }
    ) @@ timeout(5.seconds) @@ timed @@ nonFlaky(10)
}
