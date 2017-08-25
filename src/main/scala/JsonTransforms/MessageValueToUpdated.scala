package JsonTransforms

import play.api.libs.json.{JsString, JsValue, __}

/**
  * Created by Matthew.McGowan on 25/08/2017.
  */
object MessageValueToUpdated extends JsonTransform {
  def Transform(json: JsValue): Option[JsValue] = {
    val transform = (__ \ 'message).json.update(
      __.read[JsString].map { m => JsString("updated") }
    )

    json.transform(transform).asOpt
  }
}
