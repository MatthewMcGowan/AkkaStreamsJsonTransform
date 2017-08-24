import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by Matthew.McGowan on 24/08/2017.
  */
case class Msg(id: Int, message: String, otherContent: String)

object Msg {
  implicit val msgWrites: Writes[Msg] = (
    (JsPath \ "id").write[Int] and
      (JsPath \ "message").write[String] and
      (JsPath \ "otherContent").write[String]
    )(unlift (Msg.unapply))

  implicit val msgReads: Reads[Msg] = (
    (JsPath \ "id").read[Int] and
      (JsPath \ "message").read[String] and
      (JsPath \ "otherContent").read[String]
    ) (Msg.apply _)

  def Serialise(msg: Msg): String = {
    val j = Json.toJson(msg)
    Json.stringify(j)
  }

  def Deserialise(msg: String): Msg = {
    Json.parse(msg).as[Msg]
  }
}
