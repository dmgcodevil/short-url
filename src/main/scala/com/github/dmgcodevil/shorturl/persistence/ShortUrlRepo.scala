package com.github.dmgcodevil.shorturl.persistence

import scala.collection.mutable

trait ShortUrlRepo {

  /**
    * Stores the given short url and long url.
    *
    * @param shortUrl short url
    * @param longUrl  long url
    * @return a boolean `false` - if there is a value associated with the `shortUrl`
    *         before the `put` operation was executed, or `true` if `shortUrl` was not stored before
    */
  def put(shortUrl: String, longUrl: String): Boolean

  /**
    * Gets a long url for the given `shortUrl`.
    *
    * @param shortUrl short url
    * @return returns `Some(longUrl)` if there is a value associated with the `shortUrl`, otherwise None
    */
  def get(shortUrl: String): Option[String]

  /**
    * Increments clicks counter associated with the given `shortUrl`.
    *
    * @param shortUrl short url
    */
  def incrementClicks(shortUrl: String)

  /**
    * Gets number of clicks for the given `shortUrl`.
    *
    * @param shortUrl short url hash
    * @return returns `Some(numberOfClicks)` if there is a value associated with the `shortUrl`, otherwise None
    */
  def getNumberOfClicks(shortUrl: String): Option[Int]

  /**
    * Checks if there is a long url associated with the given `shortUrl`.
    *
    * @param shortUrl short url
    * @return boolean 'true' if there is a long url associated with the `shortUrl`, otherwise `false`
    */
  def contains(shortUrl: String): Boolean

  /**
    * Gets the total number of records.
    *
    * @return total number of records
    */
  def count: Long
}

/**
  * In memory implementation based on a HashMap.
  * For memory efficiency purposes this implantation works with keys of size less or equal to 9 where 7 is a length of a
  * hash value and optional 2 characters to represent a bucket number.
  * Note: this implementation is not a Thread Safe, caller is responsible for concurrency control.
  * In order to make it Thread Safe we have to use either synchronizers or concurrent hash map.
  */
object ShortUrlRepo {

  object InMemoryRepo extends ShortUrlRepo {

    import Bucket._

    private val numOfBuckets = 31

    private val buckets = Array.fill[Bucket](numOfBuckets)(new Bucket())

    override def put(shortUrl: String, longUrl: String): Boolean = {
      val bucketKey = BucketKey(shortUrl)
      val bucket = buckets(bucketKey.bucketNumber)
      if (!bucket.contains(bucketKey.hash)) {
        bucket.put(bucketKey.hash, BucketValue(longUrl, 0))
        true
      } else false
    }

    override def get(shortUrl: String): Option[String] = {
      val bucketKey = BucketKey(shortUrl)
      buckets(bucketKey.bucketNumber).get(bucketKey.hash).map(_.longUrl)
    }

    override def incrementClicks(shortUrl: String): Unit = {
      val bucketKey = BucketKey(shortUrl)
      val bucket = buckets(bucketKey.bucketNumber)
      bucket.get(bucketKey.hash) match {
        case Some(BucketValue(longUrl, numOfClicks)) => bucket.put(bucketKey.hash, BucketValue(longUrl, numOfClicks + 1))
        case None => None
      }
    }


    override def getNumberOfClicks(shortUrl: String): Option[Int] = {
      val bucketKey = BucketKey(shortUrl)
      buckets(bucketKey.bucketNumber).get(bucketKey.hash).map(_.numOfClicks)
    }


    override def contains(shortUrl: String): Boolean = {
      val bucketKey = BucketKey(shortUrl)
      buckets(bucketKey.bucketNumber).contains(bucketKey.hash)
    }

    override def count: Long = buckets.map(_.size).sum

    private class Bucket {
      private val value = mutable.Map[String, BucketValue]()

      def contains(hash: String): Boolean = value.contains(hash)

      def get(hash: String): Option[BucketValue] = value.get(hash)

      def put(hash: String, bucketValue: BucketValue): Unit = value.put(hash, bucketValue)

      def size: Int = value.size
    }

    object Bucket {

      case class BucketValue(longUrl: String, numOfClicks: Int)

      case class BucketKey(hash: String, bucketNumber: Int)

      object BucketKey {
        def apply(hash: String): BucketKey = {
          if (hash.length <= 7) BucketKey(hash, 0)
          else BucketKey(hash.substring(0, 7), hash.substring(7).toInt)
        }
      }
    }
  }

}
