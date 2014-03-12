package com.deweyvm.dogue.raven

import java.net.{Socket, ServerSocket}
import com.deweyvm.dogue.common.io.{DogueServer, NetworkData}
import com.deweyvm.dogue.common.CommonImplicits._
import scala.sys.process._
import com.deweyvm.dogue.common.logging.Log
import java.io.File
import scala.annotation.tailrec
import com.deweyvm.dogue.common.threading.Task


class Raven(timestampFile:String, lastRunFile:String, port:Int, command:Seq[String]) extends Task {
  val waitMillis = 4000
  val iterations = ((1000/waitMillis.toFloat)*120).toInt

  val server = new ServerSocket(port)
  server.setReuseAddress(true)
  override def init() {
    Log.info("Booting up")
  }

  override def doWork() {
    try {
      Log.info("Awaiting connection")
      val c = server.accept()
      Log.info("Accepted connection")
      read(c)
    } catch {
      case exc:RavenException =>
        Log.error(Log.formatStackTrace(exc))
        Thread.sleep(1000)//avoid spinning
    }
  }

  private def read(sock:Socket) {
    try {
      var reading = true
      while(reading) {
        val read = sock.receive()
        read match {
          case NetworkData.EndOfStream =>
            throw new RavenException("Client closed before sending any data")
          case NetworkData.NoneAvailable =>
            Thread.sleep(1000)
            ()
          case NetworkData.Data(s) =>
            Log.info("Got data %s" format s)
            updateServer()
            reading = false
        }
      }


      sock.transmit("Done")

      Log.info("Closing connection")
    } finally {
      sock.close()
    }
  }

  override def cleanup() {
    Log.info("Cleaning up")
    server.close()
  }

  private def restartServer() {
    Log.info("Restarting starfire")
    val out = new StringBuilder
    val err = new StringBuilder

    val logger = ProcessLogger(out.append(_).ignore(), err.append(_).ignore())
    command.lines(logger)
    out.mkString.split('\n') foreach Log.info
    err.mkString.split('\n') foreach Log.warn
  }


  private def updateServer() {
    Log.info("Updating starfire")
    tryUpdate(iterations)
  }

  @tailrec
  private def tryUpdate(iters:Int) {
    val lastModified = new File(timestampFile).lastModified()
    val lastRun = new File(lastRunFile).lastModified()
    if (lastRun < lastModified) {
      restartServer()
    } else if (iters > 0) {
      Log.verbose("Waiting for new version %d/%d" format (iters, iterations))
      Thread.sleep(waitMillis)
      tryUpdate(iters - 1)
    } else {
      throw new RavenException("Timeout")
    }
  }
}
