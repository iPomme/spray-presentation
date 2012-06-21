import cc.spray.can.model._
import cc.spray.can.server.HttpServer
import cc.spray.io.pipelines.MessageHandlerDispatch
import cc.spray.io.IoWorker
import akka.util.duration._
import akka.actor._
import akka.pattern.ask


class SimpleServer extends Actor with ActorLogging {

  import HttpMethods._

  protected def receive = {

    case HttpRequest(GET, "/", _, _, _) =>
      sender ! index

    case HttpRequest(GET, "/hello", _, _, _) =>
      sender ! response("hello!")

    case HttpRequest(GET, "/stats", _, _, _) =>
      val client = sender
      context.actorFor("../http-server").ask(HttpServer.GetStats)(1.second).onSuccess {
        case x: HttpServer.Stats => client ! statsPresentation(x)
      }

    case HttpRequest(GET, "/stop", _, _, _) =>
      sender ! response("Shutting down in 1 second ...")
      context.system.scheduler.scheduleOnce(1.second, new Runnable {
        def run() {
          context.system.shutdown()
        }
      })

  }

  def statsPresentation(s: HttpServer.Stats) = HttpResponse(
    headers = List(HttpHeader("Content-Type", "text/html")),
    body =
      <html>
        <body>
          <h1>HttpServer Stats</h1>
          <table>
            <tr>
              <td>uptime:</td> <td>
              {s.uptime.printHMS}
            </td>
            </tr>
            <tr>
              <td>totalRequests:</td> <td>
              {s.totalRequests}
            </td>
            </tr>
            <tr>
              <td>openRequests:</td> <td>
              {s.openRequests}
            </td>
            </tr>
            <tr>
              <td>maxOpenRequests:</td> <td>
              {s.maxOpenRequests}
            </td>
            </tr>
            <tr>
              <td>totalConnections:</td> <td>
              {s.totalConnections}
            </td>
            </tr>
            <tr>
              <td>openConnections:</td> <td>
              {s.openConnections}
            </td>
            </tr>
            <tr>
              <td>maxOpenConnections:</td> <td>
              {s.maxOpenConnections}
            </td>
            </tr>
            <tr>
              <td>requestTimeouts:</td> <td>
              {s.requestTimeouts}
            </td>
            </tr>
            <tr>
              <td>idleTimeouts:</td> <td>
              {s.idleTimeouts}
            </td>
            </tr>
          </table>
          <br/>
          <a href="/">Home</a>
        </body>
      </html>.toString.getBytes("ISO-8859-1")
  )

  val defaultHeaders = List(HttpHeader("Content-Type", "text/plain"))

  def response(msg: String, status: Int = 200) =
    HttpResponse(status, defaultHeaders, msg.getBytes("ISO-8859-1"))

  lazy val index = HttpResponse(
    headers = List(HttpHeader("Content-Type", "text/html")),
    body =
      <html>
        <body>
          <h1>Say hello to
            <i>spray-can</i>
            !</h1>
          <p>Defined resources:</p>
          <ul>
            <li>
              <a href="/hello">/hello</a>
            </li>
            <li>
              <a href="/stats">/stats</a>
            </li>
            <li>
              <a href="/stop">/stop</a>
            </li>
          </ul>
        </body>
      </html>.toString.getBytes("ISO-8859-1")
  )

}


object SimpleServer extends App {

  // we need an ActorSystem to host our application in
  val system = ActorSystem("SimpleHttpServer")

  // the handler actor replies to incoming HttpRequests
  val handler = system.actorOf(Props[SimpleServer])

  // every spray-can HttpServer (and HttpClient) needs an IoWorker for low-level network IO
  // (but several servers and/or clients can share one)
  val ioWorker = new IoWorker(system).start()

  // create and start the spray-can HttpServer, telling it that we want requests to be
  // handled by our singleton handler
  val server = system.actorOf(
    props = Props(new HttpServer(ioWorker, MessageHandlerDispatch.SingletonHandler(handler))),
    name = "http-server"
  )

  // a running HttpServer can be bound, unbound and rebound
  // initially to need to tell it where to bind to
  server ! HttpServer.Bind("localhost", 8080)

  // finally we drop the main thread but hook the shutdown of
  // our IoWorker into the shutdown of the applications ActorSystem
  system.registerOnTermination {
    ioWorker.stop()
  }
}