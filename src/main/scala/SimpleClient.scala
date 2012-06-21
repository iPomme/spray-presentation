import cc.spray.io.IoWorker
import cc.spray.can.client.{HttpDialog, HttpClient}
import cc.spray.can.model.HttpRequest
import akka.actor.{Props, ActorSystem}


object SimpleClient extends App {
  implicit val system = ActorSystem()
  def log = system.log

  // every spray-can HttpClient (and HttpServer) needs an IoWorker for low-level network IO
  // (but several servers and/or clients can share one)
  val ioWorker = new IoWorker(system).start()

  // create and start the spray-can HttpClient
  val httpClient = system.actorOf(
    props = Props(new HttpClient(ioWorker)),
    name = "http-client"
  )

  // create a very basic HttpDialog that results in a Future[HttpResponse]
  log.info("Dispatching GET request to localhost")
  val responseF =
    HttpDialog(httpClient, "localhost",8080)
      .send(HttpRequest(uri = "/"))
      .end

  // "hook in" our continuation
  responseF.onComplete { result =>
    result match {
      case Right(response) =>
        log.info(
          """|Result from host:
            |status : {}
            |headers: {}
            |body   : {}""".stripMargin,
          response.status, response.headers.mkString("\n  ", "\n  ", ""), response.bodyAsString
        )
      case Left(error) =>
        log.error("Could not get response due to {}", error)
    }

    log.info("Shutting down...")
    // always cleanup
    system.shutdown()
    ioWorker.stop()
  }
}