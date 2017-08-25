import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.FunSpec
import play.api.libs.json._

/**
  * Created by Matthew.McGowan on 23/08/2017.
  */
class IntegrationTests extends FunSpec with EmbeddedKafka {
  private val recordsToSend = 5
  private val outputMessages = RunAppAndCollectResults()

  describe("The AkkaStreamsJsonTransform application") {
    describe("given a message on the input kafka topic") {
      it("should place the message on the output kakfa topic, with the message updated") {
        val msg = outputMessages.head

        val expectedMsg = CreateMessageJson(1, "updated")

        assert(msg == expectedMsg)
      }
    }

    describe("given multiple messages on the input kafka topic") {
      it("should place all messages on the output kafka topic in order") {
        assert(
          outputMessages
            .map(j => (j \ "id").as[Int])
            .sliding(2)
            .forall { case Seq(x, y) => x < y }
        )
      }
    }
  }

  def RunAppAndCollectResults(): Seq[JsValue] = {
    implicit val stringSerializer = new StringSerializer
    implicit val stringDeserializer = new StringDeserializer

    val inputTopic = "testIn"
    val outputTopic = "testOut"
    val conf = ConfigFactory.load()

    EmbeddedKafka.start()
    createCustomTopic(inputTopic)
    createCustomTopic(outputTopic)

    (1 to recordsToSend)
      .map(i => CreateMessageJson(i, s"Message$i"))
      .map(Json.stringify)
      .foreach(publishToKafka(inputTopic, _))

    new StreamApp(conf).Run()

    val outputMessages = (for (i <- 1 to recordsToSend) yield {
      consumeFirstMessageFrom(outputTopic)
    }).map(Json.parse)

    EmbeddedKafka.stop()

    outputMessages
  }

  def CreateMessageJson(id: Int, message: String): JsValue = {
    JsObject(Seq(
      "id" -> JsNumber(id),
      "message" -> JsString(message),
      "otherContent" -> JsString("xxxx")
    ))
  }
}
