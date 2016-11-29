package com.bbva

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.bbva.service.DataService
import com.github.sstone.amqp.{ChannelOwner, ConnectionOwner}
import com.rabbitmq.client.ConnectionFactory
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
  val actortype = config.getString("akka.actortype")
  val logger = Logging(system, getClass)

  Http().bindAndHandle(
    Route.handlerFlow(DataService(system, rabbitConnection).route),
    config.getString("http.interface"),
    config.getInt("http.port"))

  implicit val rabbitConnection = createConnectionFactoryRabbit

  def createConnectionFactoryRabbit: ConnectionFactory = {
    val connFactory = new ConnectionFactory()
    val user = config.getString("application.rabbitmq.user")
    val pass = config.getString("application.rabbitmq.pass")
    val rabbitserver = config.getString("application.rabbitmq.server")
    connFactory.setUri(s"amqp://$user:$pass@$rabbitserver")
    connFactory
  }
}
