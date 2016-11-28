package com.bbva.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class IdElement(id: String)

object IdElement extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val responseJSON = jsonFormat1(IdElement.apply)
}

case class LogResponse(data: IdElement)

object LogResponse extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val responseJSON = jsonFormat1(LogResponse.apply)
}

case class BulkResponse(data: List[IdElement])

object BulkResponse extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val responseJSON = jsonFormat1(BulkResponse.apply)
}
