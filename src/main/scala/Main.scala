/**
  * Created by Matt on 21/08/2017.
  */
import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.util.ByteString

import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong

import Main.done
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

object Main extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val conf: Config = ConfigFactory.load()
  val consumerServers: String = conf.getString("kafka.in.bootstrapServers")
  val consumerGroup: String = conf.getString("kafka.in.groupId")
  val consumerTopic: String = conf.getString("kafka.in.topic")
  val producerServers: String = conf.getString("kafka.out.bootstrapServers")
  val producerTopic: String = conf.getString("kafka.out.topic")


  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(consumerServers)
    .withGroupId(consumerGroup)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers(producerServers)


  val done = Consumer.committableSource(consumerSettings, Subscriptions.topics(consumerTopic))
        .mapAsync(1) { msg =>
          println(msg.record.value())
          Future.successful(Done).map(_ => msg)
        }
        .map(msg =>
          ProducerMessage.Message(new ProducerRecord[Array[Byte], String](producerTopic, msg.record.value), msg.committableOffset))
        .via(Producer.flow(producerSettings))
        .map(_.message.passThrough)
        .batch(max = 20, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) =>
          batch.updated(elem)
        }
        .mapAsync(3)(_.commitScaladsl())
        .runWith(Sink.ignore)


  done.onComplete(_ => system.terminate())
}
