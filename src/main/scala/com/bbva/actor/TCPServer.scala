package com.bbva.actor

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}

class Server extends Actor {
  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8011))

  def receive = {
    case b @ Bound(localAddress) =>
      println(s"start listening port ${localAddress.getPort()}")
    case CommandFailed(command: Bind) =>
      println(command.failureMessage)
    case c @ Connected(remote, local) =>
      val handler = context.actorOf(Props[EchoHandler])
      sender() ! Register(handler)

  }
}

class EchoHandler extends Actor {

  import Tcp._

  def receive = {
    case Received(data) => print(data.decodeString("UTF8"))
    case PeerClosed => context.stop(self)
  }
}
