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

import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

object Main extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val conf: Config = ConfigFactory.load()
  val bootstrapServers: String = conf.getString("kafka.in.bootstrapServers")
  val consumerGroup: String = conf.getString("kafka.in.groupId")
  val consumerTopic: String = conf.getString("kafka.in.topic")

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServers)
    .withGroupId(consumerGroup)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val result = Consumer.committableSource(consumerSettings, Subscriptions.topics(consumerTopic))
    .mapAsync(1) { msg =>
      println(msg.record.value())
      Future.successful(Done).map(_ => msg)
    }
    .mapAsync(1) { msg =>
      msg.committableOffset.commitScaladsl()
    }
    .runWith(Sink.ignore)

  result.onComplete(_ => system.terminate())
}
