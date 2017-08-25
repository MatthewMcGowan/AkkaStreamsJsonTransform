package Flows

import akka.kafka.{ConsumerMessage, ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Flow
import org.apache.kafka.clients.producer.ProducerRecord

/**
  * Created by Matthew.McGowan on 25/08/2017.
  */
object KafkaProducerFlow {
  def GetFlow(producerTopic: String, producerSettings: ProducerSettings[Array[Byte], String]) = {
    Flow[(String, ConsumerMessage.CommittableOffset)]
      .map(msg =>
        ProducerMessage.Message(new ProducerRecord[Array[Byte], String](producerTopic, msg._1), msg._2))
      .via(Producer.flow(producerSettings))
  }
}
