import zio._
import zio.clock._
import zio.console._
import zio.duration._
import zio.stream.ZStream

object Main3 extends zio.App {

  // Layers

  type HttpClient = Has[HttpClient.Service]

  object HttpClient {

    trait Service {
      def doRequest(id: Int): ZIO[Any, Nothing, Unit]
    }

    class LiveHttpClient(console: Console.Service, clock: Clock.Service) extends Service {
      def doRequest(id: Int): ZIO[Any, Nothing, Unit] = {
        console.putStrLn(s"Fetching id ${id}") *> clock.sleep(1.seconds)
      }
    }

    def make(console: Console.Service, clock: Clock.Service): ZManaged[Console, Nothing, Service] =
      ZManaged.make(putStrLn("Acquiring http connection pool").as(new LiveHttpClient(console, clock)))(_ =>
        putStrLn("Releasing http connection pool"))

    val live = ZLayer.fromServicesManaged(make(_, _))

    def doRequest(id: Int): ZIO[HttpClient, Nothing, Unit] = ZIO.accessM(_.get.doRequest(id))
  }

  type Database = Has[Database.Service]

  object Database {

    trait Service {
      def getIds(): Task[List[Int]]
    }

    def make(): ZManaged[Console, Nothing, Service] =
      ZManaged.make(putStrLn("Acquiring database connection pool").as(new LiveDatabase))(_ =>
        putStrLn("Releasing database connection pool"))

    class LiveDatabase extends Service {
      def getIds(): Task[List[Int]] = Task.succeed(List.range(0, 10))
    }

    val live = ZLayer.fromManaged(make())

    def getIds(): ZIO[Database, Throwable, List[Int]] = ZIO.accessM(_.get.getIds())
  }

  // Program

  val program: ZIO[HttpClient with Database, Throwable, Unit] = for {
    ids <- Database.getIds()
    _   <- ZIO.foreachParN(3)(ids)(HttpClient.doRequest)
  } yield ()

  val layer: ZLayer[Clock with Console, Nothing, HttpClient with Database] = HttpClient.live ++ Database.live

  val programWithDepsProvided: ZIO[Console with Clock, Throwable, Unit] = program.provideLayer(layer)

  // Stream

  val stream: ZStream[HttpClient with Database, Throwable, Unit] =
    ZStream
      .fromIterableM(Database.getIds())
      .mapMPar(3)(HttpClient.doRequest)

  val streamWithDepsProvided: ZStream[Clock with Console, Throwable, Unit] = stream.provideLayer(layer)

  def run(args: List[String]): URIO[ZEnv, ExitCode] = streamWithDepsProvided.runDrain.exitCode
}
