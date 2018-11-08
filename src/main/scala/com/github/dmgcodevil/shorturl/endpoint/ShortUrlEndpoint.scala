package com.github.dmgcodevil.shorturl.endpoint

import com.github.dmgcodevil.shorturl.service.ShortUrlActor.Stats.GetNumberOfClicks
import com.github.dmgcodevil.shorturl.service.ShortUrlActor.{CreateShortUrl, GetLongUrl}
import akka.actor.ActorRef
import akka.http.javadsl.model.StatusCodes
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import org.apache.commons.validator.routines.UrlValidator

import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait ShortUrlEndpoint {

  def shortUrlActor: ActorRef

  def serviceHost: String

  private val defaultValidator = new UrlValidator()

  val shortUrlRoute: Route = {
    implicit val timeout = Timeout(5.seconds)
    post {
      entity(as[String]) { longUrl => {
        if (!defaultValidator.isValid(longUrl)) {
          complete(HttpResponse(entity = "invalid url format").withStatus(StatusCodes.BAD_REQUEST))
        } else {
          onComplete(shortUrlActor ? CreateShortUrl(longUrl)) {
            case Success((shortUrl: String, created: Boolean)) =>
              complete(HttpResponse(entity = s"$serviceHost/$shortUrl").withStatus(if (created) StatusCodes.CREATED else StatusCodes.OK))
            case Failure(throwable) => complete(HttpResponse(entity = throwable.getMessage).withStatus(StatusCodes.INTERNAL_SERVER_ERROR))
          }
        }
      }
      }
    } ~ get {
      path(Segment) { longUrl =>
        implicit val timeout = Timeout(5.seconds)
        onComplete(shortUrlActor ? GetLongUrl(longUrl)) {
          case Success(Some(value: String)) => complete(HttpResponse(entity = value).withStatus(StatusCodes.OK))
          case Success(None) => complete(HttpResponse(entity = s"long url have not found").withStatus(StatusCodes.NOT_FOUND))
          case Failure(throwable) => complete(HttpResponse(entity = throwable.getMessage).withStatus(StatusCodes.INTERNAL_SERVER_ERROR))
        }
      }
    } ~ pathPrefix("api") {
      get {
        path("stats" / "clicks" / Segment) { shortUrl =>
          implicit val timeout = Timeout(5.seconds)
          onComplete(shortUrlActor ? GetNumberOfClicks(shortUrl)) {
            case Success(Some(value: Int)) => complete(HttpResponse(entity = value.toString).withStatus(StatusCodes.OK))
            case Success(None) => complete(HttpResponse(entity = s"long url have not found").withStatus(StatusCodes.NOT_FOUND))
            case Failure(throwable) => complete(HttpResponse(entity = throwable.getMessage).withStatus(StatusCodes.INTERNAL_SERVER_ERROR))
          }
        }
      }
    }
  }
}
