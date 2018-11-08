package com.github.dmgcodevil.shorturl.service

import com.github.dmgcodevil.shorturl.persistence.ShortUrlRepo

class ShortUrlService(encoder: Encoder, repo: ShortUrlRepo) {

  def createShort(longUrl: String): (String, Boolean) = {
    val shortUrl = encoder.encode(longUrl)
    if (repo.contains(shortUrl)) {
      (shortUrl, false)
    } else {
      repo.put(shortUrl, longUrl)
      (shortUrl, true)
    }
  }

  def getLongUrl(shortUrl: String): Option[String] = {
    repo.get(shortUrl) match {
      case s@Some(_) =>
        repo.incrementClicks(shortUrl)
        s
      case _ => None
    }
  }

  def getNumberOfClicks(shortUrl: String): Option[Int] = {
    repo.getNumberOfClicks(shortUrl)
  }

  def count: Long = repo.count
}

object ShortUrlService {
  def apply(encoder: Encoder, repo: ShortUrlRepo): ShortUrlService = new ShortUrlService(encoder, repo)
}
