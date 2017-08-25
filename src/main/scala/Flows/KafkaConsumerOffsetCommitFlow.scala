package Flows

import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.{ConsumerMessage, ProducerMessage}
import akka.stream.scaladsl.Flow

/**
  * Created by Matthew.McGowan on 25/08/2017.
  */
object KafkaConsumerOffsetCommitFlow {
  def GetFlow(maxBatchSize: Int) = {
    Flow[ProducerMessage.Result[Array[Byte], String, ConsumerMessage.CommittableOffset]]
      .map(_.message.passThrough)
      .batch(max = maxBatchSize, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) =>
        batch.updated(elem)
      }
      .mapAsync(3)(_.commitScaladsl())
  }
}
