import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.FunSpec

/**
  * Created by Matthew.McGowan on 23/08/2017.
  */
class IntegrationTests extends FunSpec with EmbeddedKafka {
  // TODO: Remove dependency on Msg case class. Rely on JsObject instead.

  implicit val stringSerializer = new StringSerializer
  implicit val stringDeserializer = new StringDeserializer

  private val inputTopic = "testIn"
  private val outputTopic = "testOut"
  private val recordsToSend = 5
  private val conf = ConfigFactory.load()

  private val inputMessages = (1 to recordsToSend).map(i => Msg(i, s"Message$i", "x"))

  EmbeddedKafka.start()
  createCustomTopic(inputTopic)
  createCustomTopic(outputTopic)

  inputMessages
    .map(Msg.Serialise)
    .foreach(publishToKafka(inputTopic, _))

  new StreamApp(conf).Run()

  private val outputMessages = (for (i <- 1 to recordsToSend) yield {
    consumeFirstMessageFrom(outputTopic)
  }).map(Msg.Deserialise)

  EmbeddedKafka.stop()

  describe("The AkkaStreamsJsonTransform application") {
    describe("given a message on the input kafka topic") {
      it("should place the message on the output kakfa topic, with the message updated") {
        val msg = outputMessages.head

        val expectedMsg = Msg(1, "updated", "x")

        assert(msg == expectedMsg)
      }
    }

    describe("given multiple messages on the input kafka topic") {
      it("should place all messages on the output kafka topic in order") {
        assert(
          outputMessages
            .map(_.id)
            .sliding(2)
            .forall { case Seq(x, y) => x < y }
        )
      }
    }
  }
}
