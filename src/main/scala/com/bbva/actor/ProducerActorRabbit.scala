package com.bbva.actor

import akka.actor.Actor
import com.bbva.App
import com.bbva.service.{Message, SyncMessage}
import com.github.sstone.amqp.Amqp.Publish
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

class ProducerActorRabbit extends Actor {

  val config = ConfigFactory.load()

  private def logger = LoggerFactory.getLogger(this.getClass)

  val producer = App.getRabbitProducer


  override def receive: Receive = {
    case Message(topic, id, message) =>
      producer ! Publish(topic, id, message.getBytes, properties = None, mandatory = true, immediate = false)
    case SyncMessage(topic, id, message) =>
      val response = producer ! Publish(topic, id, message.getBytes, properties = None, mandatory = true, immediate = false)
      logger.debug("response: " + response)

  }
}