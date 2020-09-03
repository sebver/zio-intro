import zio._
import zio.clock._
import zio.console._
import zio.duration._
import zio.random._

object Main1 { // extends App {

  def playGame() = {

    val randomNumber = scala.util.Random.between(0, 10)

    val input = scala.io.StdIn.readInt()

    val won = input == randomNumber

    if (won) {
      println("You won")
    } else {
      println(s"You lost, the number was $randomNumber")
    }
  }

  // Basic + error channel

  val program0: IO[String, Boolean] = (for {
    randomNumber <- Task(scala.util.Random.between(0, 10))
    _            <- Task(println("Please enter a random number between 0 and 10:"))
    input        <- Task(scala.io.StdIn.readInt())
    won          = input == randomNumber

    _ <- Task(if (won) {
          println("You won")
        } else {
          println(s"You lost, the number was $randomNumber")
        })
  } yield won).orElseFail("Errrooorrrr")

  // Basic + retry + can't fail

  val program1: IO[Nothing, Boolean] = (for {
    randomNumber <- Task(scala.util.Random.between(0, 10))
    _            <- Task(println("Please enter a random number between 0 and 10:"))
    input        <- Task(scala.io.StdIn.readInt())
    won          = input == randomNumber

    _ <- Task(if (won) {
          println("You won")
        } else {
          println(s"You lost, the number was $randomNumber")
        })

  } yield won).retryN(2).orElseSucceed(false)

  // ZIO environment

  val program2: ZIO[Console with Random, Nothing, Boolean] = (for {
    randomNumber <- nextIntBetween(0, 10)
    _            <- putStrLn("Please enter a random number between 0 and 10:")
    input        <- getStrLn.flatMap(str => Task(str.toInt))
    won          = input == randomNumber

    _ <- if (won) {
          putStrLn("You won")
        } else {
          putStrLn(s"You lost, the number was $randomNumber")
        }

  } yield won).retryN(2).orElseSucceed(false)

  // Schedules

  val program3: URIO[Console with Random with Clock, Boolean] = (for {
    randomNumber <- nextIntBetween(0, 10)
    _            <- putStrLn("Please enter a random number between 0 and 10:")
    input        <- getStrLn.flatMap(str => Task(str.toInt))
    won          = input == randomNumber

    _ <- if (won) {
          putStrLn("You won")
        } else {
          putStrLn(s"You lost, the number was $randomNumber")
        }

  } yield won).retry(Schedule.spaced(2.seconds)).orElseSucceed(false)

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
//    program.as(ExitCode.success)
    program0
      .tapError(err => Task(println(err)))
      .fold(_ => ExitCode.failure, _ => ExitCode.failure)
}
