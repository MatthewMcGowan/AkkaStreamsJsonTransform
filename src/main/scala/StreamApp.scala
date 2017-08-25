import Flows.{KafkaConsumerOffsetCommitFlow, KafkaProducerFlow, TransformFlow}
import JsonTransforms.MessageValueToUpdated
import akka.actor.ActorSystem
import akka.kafka._
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

/**
  * Created by Matthew.McGowan on 23/08/2017.
  */
class StreamApp(conf: Config) {
  implicit val system = ActorSystem("QuickStart")
  implicit val materialiser = ActorMaterializer()
  implicit val ec = system.dispatcher

  val consumerServers: String = conf.getString("kafka.in.bootstrapServers")
  val consumerGroup: String = conf.getString("kafka.in.groupId")
  val consumerTopic: String = conf.getString("kafka.in.topic")
  val offsetCommitMaxBatch: Int = conf.getInt("kafka.in.offsetCommitMaxBatchSize")
  val producerServers: String = conf.getString("kafka.out.bootstrapServers")
  val producerTopic: String = conf.getString("kafka.out.topic")

  private val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(consumerServers)
    .withGroupId(consumerGroup)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(producerServers)

  private val transforms = Seq(MessageValueToUpdated)

  def Run(): Unit = {
    val done =
      Consumer.committableSource(consumerSettings, Subscriptions.topics(consumerTopic))
        .via(TransformFlow.GetFlow(transforms))
        .via(KafkaProducerFlow.GetFlow(producerTopic, producerSettings))
        .via(KafkaConsumerOffsetCommitFlow.GetFlow(offsetCommitMaxBatch))
        .runWith(Sink.ignore)

    done.onComplete(_ => system.terminate())
  }
}
