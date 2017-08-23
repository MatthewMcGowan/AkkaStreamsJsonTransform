import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.scalatest.FunSpec

/**
  * Created by Matthew.McGowan on 23/08/2017.
  */
class MessagePassedUnchanged extends FunSpec with EmbeddedKafka {
  implicit val stringSerializer = new StringSerializer
  implicit val stringDeserializer = new StringDeserializer

  private val inputTopic = "testIn"
  private val outputTopic = "testOut"

  describe("The AkkaStreamsJsonTransform application") {
    describe("given a message on the input kafka topic") {
      it("should place the message unchanged on the output kakfa topic") {
        withRunningKafka {
          createCustomTopic(inputTopic)
          createCustomTopic(outputTopic)
          publishToKafka(inputTopic, "TestMessage")

          val stream = new StreamApp
          stream.Run()

          val msg = consumeFirstMessageFrom(outputTopic)

          assert(msg == "TestMessage")
        }
      }
    }

    describe("given multiple messages on the input kafka topic") {
      it("should place all messages unchanged on the output kafka topic in order") {
        withRunningKafka {
          createCustomTopic(inputTopic)
          createCustomTopic(outputTopic)
          publishToKafka(inputTopic, "Message1")
          publishToKafka(inputTopic, "Message2")

          val stream = new StreamApp
          stream.Run()

          val firstMessage = consumeFirstMessageFrom(outputTopic)
          val secondMessage = consumeFirstMessageFrom(outputTopic)

          assert(firstMessage == "Message1")
          assert(secondMessage == "Message2")
        }
      }
    }
  }
}
