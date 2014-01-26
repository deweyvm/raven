package com.deweyvm.dogue.raven

import java.net.{Socket, ServerSocket}
import com.deweyvm.dogue.common.io.NetworkData
import com.deweyvm.dogue.common.Implicits._
import scala.sys.process._
import com.deweyvm.dogue.common.logging.Log
import java.io.File
import scala.annotation.tailrec



class Raven(timestampFile:String, lastRunFile:String, command:Seq[String]){
  val waitMillis = 4000
  val iterations = ((1000/waitMillis.toFloat)*120).toInt
  var running = true
  def execute() {
    val port = 27181
    val backlog = 1
    val server = new ServerSocket(port, backlog)
    server.setReuseAddress(true)
    var connection:Option[Socket] = None
    while(running) {
      try {
        Log.info("Awaiting connection")
        connection = server.accept().some
        Log.info("Accepted connection")
        var reading = true
        while(reading) {
          connection foreach { sock =>
            val read = sock.receive()
            read match {
              case NetworkData.EndOfStream =>
                throw new RavenException("Client closed before sending any data")
              case NetworkData.NoneAvailable =>
                Thread.sleep(100)
                ()
              case NetworkData.Data(s) =>
                Log.info("Got data %s" format s)
                updateServer()
                reading = false
            }
          }
        }

        connection foreach {
          _.transmit("Done")
        }
        Log.info("Closing connection")
        connection foreach {_.close()}
        connection = None
      } catch {
        case exc:RavenException =>
          Log.error(Log.formatStackTrace(exc))
          Thread.sleep(100)//avoid spinning
      } finally {
        connection foreach {_.close()}
      }
    }


  }

  private def restartServer() {
    Log.info("Restarting starfire")
    val out = new StringBuilder
    val err = new StringBuilder

    val logger = ProcessLogger(out.append(_), err.append(_))
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
      Log.info("Waiting for new version %d/%d" format (iters, iterations))
      Thread.sleep(waitMillis)
      tryUpdate(iters - 1)
    } else {
      throw new RavenException("Timeout")
    }
  }
}
