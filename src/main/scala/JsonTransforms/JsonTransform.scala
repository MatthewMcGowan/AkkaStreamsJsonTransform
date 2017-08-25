package JsonTransforms

import play.api.libs.json.JsValue

/**
  * Created by Matthew.McGowan on 25/08/2017.
  */
trait JsonTransform {
  def Transform(json: JsValue): Option[JsValue]
}
