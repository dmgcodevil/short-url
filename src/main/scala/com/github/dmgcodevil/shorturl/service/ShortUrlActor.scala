package com.github.dmgcodevil.shorturl.service

import com.github.dmgcodevil.shorturl.service.ShortUrlActor.Stats.GetNumberOfClicks
import com.github.dmgcodevil.shorturl.service.ShortUrlActor._
import akka.actor.{Actor, Props}


class ShortUrlActor(service: ShortUrlService) extends Actor {
  override def receive: Receive = {
    case CreateShortUrl(longUrl) => sender() ! service.createShort(longUrl)
    case GetLongUrl(shortUrl) => sender() ! service.getLongUrl(shortUrl)
    case GetNumberOfClicks(shortUrl) => sender() ! service.getNumberOfClicks(shortUrl)
  }
}

object ShortUrlActor {

  case class CreateShortUrl(longUrl: String)

  case class GetLongUrl(shortUrl: String)

  sealed trait Stats

  object Stats {

    case class GetNumberOfClicks(shortUrl: String) extends Stats

  }

  def props(service: ShortUrlService) = Props(new ShortUrlActor(service))

}