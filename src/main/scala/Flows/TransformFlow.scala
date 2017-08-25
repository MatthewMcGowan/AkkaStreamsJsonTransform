package Flows

import JsonTransforms.JsonTransform
import akka.Done
import akka.kafka.ConsumerMessage
import akka.stream.scaladsl.Flow
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Created by Matthew.McGowan on 25/08/2017.
  */
object TransformFlow {
  def GetFlow(transforms: Seq[JsonTransform])(implicit executor: ExecutionContext) = {
    Flow[ConsumerMessage.CommittableMessage[Array[Byte], String]]
      .mapAsync(1) { msg =>
        val msgValue = msg.record.value()
        val jValue = Try(Json.parse(msgValue)).toOption

        val jOut: String = transforms
          .foldLeft(jValue)((j, t) => j.flatMap(t.Transform))
          .map(Json.stringify)
          .getOrElse(msg.record.value())

        //println(s"${msg.record.value()} transformed to $j")
        Future.successful(Done).map(_ => (jOut, msg.committableOffset))
      }
  }
}
