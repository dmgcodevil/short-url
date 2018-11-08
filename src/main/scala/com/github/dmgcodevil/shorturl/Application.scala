package com.github.dmgcodevil.shorturl

import com.github.dmgcodevil.shorturl.endpoint.ShortUrlEndpoint
import com.github.dmgcodevil.shorturl.persistence.ShortUrlRepo
import com.github.dmgcodevil.shorturl.service.{Encoder, ShortUrlActor, ShortUrlService}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object Application extends App with ShortUrlEndpoint {

  val log = Logger("ShortUrlService")
  val host = "127.0.0.1"
  val port = 8080
  val shortUrlService = ShortUrlService(Encoder.MD5Encoder, ShortUrlRepo.InMemoryRepo)
  implicit val sys: ActorSystem = ActorSystem("akka-http-app")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = sys.dispatcher

  override def shortUrlActor: ActorRef = sys.actorOf(ShortUrlActor.props(shortUrlService))

  Http().bindAndHandle(shortUrlRoute, "127.0.0.1", 8080).onComplete {
    case Success(b) => log.info(s"application is up and running at ${b.localAddress.getHostName}:${b.localAddress.getPort}")
    case Failure(e) => log.error(s"could not start application: {}", e.getMessage)
  }

  override def serviceHost: String = s"$host:$port"
}
