import Main3._
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.environment._

object IntegrationTest extends DefaultRunnableSpec {
  def spec: ZSpec[TestEnvironment, Any] =
    suite("Tests")(
      testM("Test the database") {
        assertM(Database.getIds())(equalTo(List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)))
      },
      testM("Test the http client") {
        assertM(HttpClient.doRequest(1))(isUnit)
      }
    ).provideLayerShared((ZEnv.live >>> layer).retry(Schedule.forever))
}
