package com.bbva.actor

import java.util.Properties

import akka.actor.Actor
import com.bbva.service.{SyncMessage, Message}
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

class ProducerActor extends Actor {

  val config = ConfigFactory.load()

  private def logger = LoggerFactory.getLogger(this.getClass)

  override def receive: Receive = {
    case Message(topic, id, message) =>
      "OK"
    case SyncMessage(topic, id, message) =>
      "OK"
  }

  private def getConfiguration: Properties = {

    val props: Properties = new Properties
    props
  }
}
