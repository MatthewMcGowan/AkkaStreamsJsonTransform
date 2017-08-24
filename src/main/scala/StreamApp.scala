import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}
import play.api.libs.json.Reads._
import play.api.libs.json.{JsString, JsValue, Json, _}

import scala.concurrent.Future
import scala.util.Try

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

        def modifyJsonMessage(json: JsValue): Option[JsValue] = {
          val transform = (__ \ 'message).json.update(
            __.read[JsString].map{m => JsString("updated")}
          )

          json.transform(transform).asOpt
        }

        val j = Try(Json.parse(msg.record.value())).toOption
          .flatMap(j => modifyJsonMessage(j))
          .map(Json.stringify)
          .getOrElse(msg.record.value())

        println(s"${msg.record.value()} transformed to $j")
        Future.successful(Done).map(_ => (j, msg.committableOffset))
      }
      .map(msg =>
        ProducerMessage.Message(new ProducerRecord[Array[Byte], String](producerTopic, msg._1), msg._2))
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
