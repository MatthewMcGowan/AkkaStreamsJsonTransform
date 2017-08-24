import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.concurrent.Future

/**
  * Created by Matthew.McGowan on 23/08/2017.
  */
class StreamApp(conf: Config) {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val consumerServers: String = conf.getString("kafka.in.bootstrapServers")
  val consumerGroup: String = conf.getString("kafka.in.groupId")
  val consumerTopic: String = conf.getString("kafka.in.topic")
  val producerServers: String = conf.getString("kafka.out.bootstrapServers")
  val producerTopic: String = conf.getString("kafka.out.topic")
  val producerMaxBatch: Int = conf.getInt("kafka.out.maxBatchSize")


  private val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(consumerServers)
    .withGroupId(consumerGroup)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(producerServers)

  def Run(): Unit = {
    val done = Consumer.committableSource(consumerSettings, Subscriptions.topics(consumerTopic))
      .mapAsync(1) { msg =>
        println(msg.record.value())
        Future.successful(Done).map(_ => msg)
      }
      .map(msg =>
        ProducerMessage.Message(new ProducerRecord[Array[Byte], String](producerTopic, msg.record.value), msg.committableOffset))
      .via(Producer.flow(producerSettings))
      .map(_.message.passThrough)
      .batch(max = producerMaxBatch, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) =>
        batch.updated(elem)
      }
      .mapAsync(3)(_.commitScaladsl())
      .runWith(Sink.ignore)

    done.onComplete(_ => system.terminate())
  }
}
