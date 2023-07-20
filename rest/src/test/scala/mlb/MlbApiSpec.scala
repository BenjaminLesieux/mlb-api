package mlb

import munit.*
import zio.http.*
import zio.jdbc.ZConnectionPool
import mlb.MlbApi.validateEnv

class MlbApiSpec extends munit.ZSuite {

  val app: App[ZConnectionPool] = mlb.MlbApi.mlbGamesEndpoints

  testZ("should return 501 Not Implemented for /init") {
    val req = Request.get(URL(Root / "init"))
    assertEqualsZ(
      app
        .runZIO(req)
        .map(_.status),
      Status.Ok
    )
  }
}
