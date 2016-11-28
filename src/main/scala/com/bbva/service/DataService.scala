package com.bbva.service

import java.util.Optional

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route}
import akka.pattern.ask
import akka.routing.FromConfig
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.{CorsDirectives, CorsSettings}
import com.bbva.actor.ProducerActor
import com.bbva.model.{BulkLog, BulkResponse, IdElement, LogResponse}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Message to sent to Kafka.
  *
  * @param topic   Destination topic.
  * @param id      MessageId autogenerated.
  * @param message Message body.
  */
case class Message(topic: String, id: String, message: String)

case class SyncMessage(topic: String, id: String, message: String)

case class DataService(system: ActorSystem)(implicit val config: Config) {

  implicit val timeout = Timeout(5 seconds)

  val router = system.actorOf(FromConfig.props(Props[ProducerActor]), "mainRouter")

  val settings = CorsSettings.defaultSettings.copy(allowGenericHttpRequests = false)

  val route: Route = handleRejections(CorsDirectives.corsRejectionHandler) {
    pathPrefix("mike") {
      path("healthCheck") {
        get {
          complete(StatusCodes.OK)
        }
      }
    } ~
      cors(settings) {
        handleRejections(RejectionHandler.default) {
          pathPrefix("mike") {
            path("structured-logs") {
              pathEndOrSingleSlash {
                post {
                  entity(as[List[BulkLog]]) { events =>
                    val uuids = events.map(event => sendLogToProducer(event.sourceSystem, event.message))
                    bulkResponse("structured-logs", uuids)
                  }
                }
              }
            } ~
              path("unstructured-logs") {
                pathEndOrSingleSlash {
                  post {
                    entity(as[List[BulkLog]]) { events =>
                      val uuids = events.map(event => sendLogToProducer(event.sourceSystem, event.message))
                      bulkResponse("unstructured-logs", uuids)
                    }
                  }
                }
              } ~
              pathPrefix("structured-logs" / Segment) { source =>
                pathEndOrSingleSlash {
                  post {
                    entity(as[String]) { event =>
                      val uuid = sendLogToProducer(source, event)
                      logResponse("structured-logs", uuid)
                    }
                  }
                }
              } ~
              pathPrefix("unstructured-logs" / Segment) { source =>
                pathEndOrSingleSlash {
                  post {
                    entity(as[String]) { event =>
                      val uuid = sendLogToProducer(source, event)
                      logResponse("unstructured-logs", uuid)
                    }
                  }
                }
              } ~
              path("traceabilities") {
                pathEndOrSingleSlash {
                  post {
                    entity(as[String]) { event =>
                      sendTraceabilityToProducer(event)
                    }
                  }
                }
              }

          }
        }
      }
  }

  def sendTraceabilityToProducer(event: String) = {
    val destTopic = getTraceabilityTopic
    val newUUID = uuidGenerator
    val future = (router ? SyncMessage(destTopic, newUUID, event)).mapTo[Boolean]
    val result = Await.result(future, 5 seconds)

    val headers = List(
      RawHeader("Location", s"/mike/traceabilities/$newUUID"),
      RawHeader("Content-Location", s"/mike/traceabilities")
    )
    if (result) {
      respondWithHeaders(headers) {
        complete((StatusCodes.OK, LogResponse(IdElement(newUUID))))
      }
    } else {
      respondWithHeaders(headers) {
        complete((StatusCodes.InternalServerError))
      }
    }
  }

  def getTraceabilityTopic:String={
      "default"
  }

  def getTopicName(source: String): String = {
    "default"
  }

  def sendLogToProducer(source: String, event: String) = {
    val newUUID = uuidGenerator
    router ! Message(getTopicName(source), newUUID, event)
    newUUID
  }

  def logResponse(path: String, newUUID: String) = {
    val headers = List(
      RawHeader("Content-Location", s"/mike/$path")
    )
    respondWithHeaders(headers) {
      complete((StatusCodes.Accepted, LogResponse(IdElement(newUUID))))
    }
  }

  def bulkResponse(path: String, newUUIDs: List[String]) = {
    val headers = List(
      RawHeader("Content-Location", s"/mike/$path")
    )
    respondWithHeaders(headers) {
      complete((StatusCodes.Accepted, BulkResponse(newUUIDs.map(IdElement(_)))))
    }
  }

  def uuidGenerator = java.util.UUID.randomUUID.toString

}
