import zio._
import zio.clock._
import zio.console._
import zio.duration._
import zio.stream.ZStream

object Main2 extends zio.App {

  // Resource safety

  class HttpClient private () {

    def doRequest(id: Int): ZIO[Clock with Console, Nothing, Unit] = {
      putStrLn(s"Fetching id ${id}") *> sleep(1.seconds)
    }
  }

  object HttpClient {

    def make(): ZManaged[Console, Nothing, HttpClient] =
      ZManaged.make(putStrLn("Acquiring http connection pool").as(new HttpClient))(_ =>
        putStrLn("Releasing http connection pool"))
  }

  class Database private () {

    def getIds(): Task[List[Int]] = Task.succeed(List.range(0, 10))
  }

  object Database {

    def make(): ZManaged[Console, Nothing, Database] =
      ZManaged.make(putStrLn("Acquiring database connection pool").as(new Database))(_ =>
        putStrLn("Releasing database connection pool"))
  }

  case class Dependencies(db: Database, httpClient: HttpClient)

  object Dependencies {
    def make(): ZManaged[Console, Nothing, Dependencies] =
      for {
        db         <- Database.make()
        httpClient <- HttpClient.make()
      } yield Dependencies(db, httpClient)
  }

  // Using a managed

  val program: ZIO[Clock with Console, Throwable, Unit] =
    Dependencies.make().use { deps =>
      for {
        ids <- deps.db.getIds()
        _   <- ZIO.foreach(ids)(deps.httpClient.doRequest)
      } yield ()
    }

  // Streams are built on managed

  val stream: ZStream[Clock with Console, Throwable, Unit] =
    ZStream.managed(Dependencies.make()).flatMap { deps =>
      ZStream
        .fromIterableM(deps.db.getIds())
        .mapMPar(3)(deps.httpClient.doRequest)
    }

  def run(args: List[String]): URIO[ZEnv, ExitCode] = program.exitCode
}
