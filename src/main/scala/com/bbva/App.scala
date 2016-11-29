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

  val producer:ActorRef = if(actortype=="rabbit"){
    val connFactory = new ConnectionFactory()
    val user = config.getString("akka.rabbituser")
    val pass = config.getString("akka.rabbitpass")
    connFactory.setUri(s"amqp://$user:$pass@localhost")
    val conn = system.actorOf(ConnectionOwner.props(connFactory, 1 second))
    ConnectionOwner.createChildActor(conn, ChannelOwner.props())
  }else{
    null
  }

  def getRabbitProducer = {
    producer
  }



  Http().bindAndHandle(
    Route.handlerFlow(DataService(system).route),
    config.getString("http.interface"),
    config.getInt("http.port"))
}
