package com.github.dmgcodevil.shorturl.service

import java.nio.ByteBuffer
import java.security.MessageDigest


trait Encoder {
  def encode(longUrl: String): String
}

object Encoder {

  /**
    * This implementation uses MD5 128 encoding and creates 7 length hash code. Two additional characters are used to represent
    * a bucket number.
    * Algorithm:
    * 1. Encode long url using MD5
    * 2. Take the first 42 bits and convert to a decimal of base 2
    * 3. Convert a decimal from the second step to string using base 62 ([a-z] - 26, [A-Z] - 26, [0, 9] -> 10)
    * 4. Convert 128 bits to a long number and calculate bucket using the following formula (number % hash.length + 15)
    * 5. Append bucket number to the hash
    *
    */
  object MD5Encoder extends Encoder {
    val offset = 15 // we need add offset because md5 hash number can be negative
    val shortUrlSizeInBits = 42
    val md5: MessageDigest = MessageDigest.getInstance("MD5")
    val charset = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    override def encode(longUrl: String): String = {
      md5.reset()
      md5.update(longUrl.getBytes("UTF-8"))
      val hash = md5.digest()
      val base62 = asBase62(hash)
      val bucketNum = calcBucketNum(hash)
      s"$base62$bucketNum"
    }

    // calculates the number of bucket for the given 128 bit hash code
    private def calcBucketNum(hash: Array[Byte]): Int = {
      (ByteBuffer.wrap(hash).getLong % hash.length + offset).toInt
    }

    def asBase62(hash: Array[Byte]): String = {
      val bits = new StringBuilder()
      val numberOfBytes = 6 // (42 / 8) and round up

      bits.append((0 to numberOfBytes).map(i => toBinaryString(hash(i))).mkString(""))
      val str = bits.toString().substring(0, shortUrlSizeInBits)
      val decimal = java.lang.Long.parseLong(str, 2)
      var tmp = decimal
      val stringHash = new StringBuilder
      val base = 62
      var shortUrlLen = 7
      var tail = hash.length - 1
      while (shortUrlLen > 0) {
        if (tmp > 0) {
          stringHash.append(charset.charAt((tmp % base).toInt))
          tmp /= base
        } else {
          // if we run out of digits start taking bytes from the end
          stringHash.append(charset.charAt(Integer.parseInt(toBinaryString(hash(tail)), 2) % base))
          tail -= 1
        }
        shortUrlLen -= 1

      }
      stringHash.toString()
    }

  }

  def toBinaryString(byte: Byte): String = Integer.toBinaryString((byte & 0xFF) + 0x100).substring(1)


}