# AkkaStreamsJsonTransform
Akka Streams used to apply transforms to JSON data, read from and written to Kafka.

# Functionality
It is designed as a framework to take any JSON message from and input Kafka Topic, transform it, and write it out to an output Kafka Topic. It does this using Akka Streams.

Currently, it expects a JSON message with a string field called "message":

```json
{
  "message" : "value" 
}
```

The JSON may contain arbitrary other section which will be passed through unmodified:

```json
{
  "ignoredField": 1,
  "message": "xxxx",
  "ignoredJObject": {
    "field": "value"
  }
}
```

The only transform currently in the project takes the "message" field and replaces the value with "updated". It can be extended to apply any number of transforms in order, defined with the following trait:

```scala
trait JsonTransform {
  def Transform(json: JsValue): Option[JsValue]
}
```

# Dependencies
The two key dependencies for the project are:
 - Akka Streams, with it's associate Kafka connector
 - Play-Json, which is used to represent Json internally 
 