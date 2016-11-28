package com.bbva

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.bbva.service.DataService
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._


/**
  * @author ${user.name}
  */
object App extends scala.App {
  implicit val config = ConfigFactory.load()

  implicit val system = ActorSystem("rest")
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(5 seconds)
  val logger = Logging(system, getClass)

  Http().bindAndHandle(
    Route.handlerFlow(DataService(system).route),
    config.getString("http.interface"),
    config.getInt("http.port"))
}
