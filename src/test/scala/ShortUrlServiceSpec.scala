import com.github.dmgcodevil.shorturl.persistence.ShortUrlRepo
import com.github.dmgcodevil.shorturl.service.{Encoder, ShortUrlService}
import com.typesafe.scalalogging.Logger
import net.andreinc.mockneat.MockNeat
import net.andreinc.mockneat.types.enums.{DomainSuffixType, HostNameType, URLSchemeType}
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._


class ShortUrlServiceSpec extends FlatSpec {

  private val service = ShortUrlService(Encoder.MD5Encoder, ShortUrlRepo.InMemoryRepo)
  private val mockNeat = MockNeat.threadLocal

  private val logger = Logger(LoggerFactory.getLogger(classOf[ShortUrlServiceSpec]))


  "ShortUrlService" should "generate short url" in {
    val numberOfSamples = 1000000

    val urls = generateRandomUrls(numberOfSamples)
    val numOfUniqueUrl = urls.toSet.size
    for (longUrl <- urls) {
      val shortUrl = service.createShort(longUrl)._1
      val actual = service.getLongUrl(shortUrl)
      actual.value should equal(longUrl)
    }
    service.count should equal(numOfUniqueUrl)
    logger.info(s"total urls processed: $numberOfSamples")
    logger.info(s"total unique urls: $numOfUniqueUrl")
  }

  def generateRandomUrls(numberOfUrls: Int): Iterable[String] =
    mockNeat.urls()
      .scheme(URLSchemeType.HTTPS)
      .domain(DomainSuffixType.POPULAR)
      .host(HostNameType.ADVERB_VERB)
      .list(numberOfUrls)
      .`val`().asScala

}
