package com.bbva.actor

import akka.actor.{Actor, ActorRef, ActorSystem}
import com.bbva.App
import com.bbva.service.{Message, SyncMessage}
import com.github.sstone.amqp.Amqp.Publish
import com.github.sstone.amqp.{ChannelOwner, ConnectionOwner}
import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

class ProducerActorRabbit(connFactory: ConnectionFactory, system: ActorSystem) extends Actor {

  val config = ConfigFactory.load()

  private def logger = LoggerFactory.getLogger(this.getClass)

  createRabbitProducer

  def createRabbitProducer: ActorRef = {
    val conn = system.actorOf(ConnectionOwner.props(connFactory, 1 second))
    ConnectionOwner.createChildActor(conn, ChannelOwner.props())
  }

  val producer = createRabbitProducer


  override def receive: Receive = {
    case Message(topic, id, message) =>
      producer ! Publish(topic,
                          id,
                          message.getBytes,
                          properties = None,
                          mandatory = true,
                          immediate = false)
    case SyncMessage(topic, id, message) =>
      val response = producer ! Publish(topic,
                                          id, message.getBytes,
                                          properties = None,
                                          mandatory = true,
                                          immediate = false)
      logger.debug("response: " + response)
  }
}
