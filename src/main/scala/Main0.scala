import scala.concurrent.Future

object Main0 extends App {

//  import scala.concurrent.ExecutionContext.Implicits.global
//
//  val future1 = Future { println("Future 1") }
//
//  val future2 = Future { println("Future 2") }
//
//  val result = for {
//    _ <- future1
//    _ <- future2
//  } yield ()

  case class BetterFuture[A](run: () => A) {

    def map[B](f: A => B): BetterFuture[B] = BetterFuture(() => f(run()))

    def flatMap[B](f: A => BetterFuture[B]): BetterFuture[B] = BetterFuture(() => f(run()).run())

    def twice: BetterFuture[A] =
      BetterFuture(() => {
        run()
        run()
      })
  }

  val bf1 = BetterFuture(() => println("Better future 1"))

  val bf2 = BetterFuture(() => println("Better future 2"))

  val result = for {
    _ <- BetterFuture(() => println("Better future 1"))
    _ <- bf2
  } yield ()

  result.twice.run()

}
