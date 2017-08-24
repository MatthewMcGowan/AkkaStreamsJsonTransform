import com.typesafe.config.ConfigFactory

/**
  * Created by Matt on 21/08/2017.
  */

object Main extends App {
  val conf = ConfigFactory.load()
  val stream = new StreamApp(conf)

  stream.Run()
}
