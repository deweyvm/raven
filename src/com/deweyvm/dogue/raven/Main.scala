package com.deweyvm.dogue.raven

import com.deweyvm.dogue.common.logging.Log
import java.lang.management.ManagementFactory

case class RavenOptions(logDir:String=".", timestamp:String=".", lastRunFile:String=".", command:String="")

object Main {
  def main(args:Array[String]) {
    val parser = new scopt.OptionParser[RavenOptions]("raven") {
      head("raven", "testing.0")

      opt[String]("log") action { (x, c) =>
        c.copy(logDir = x)
      } text "directory to place logs"

      opt[String]("timestamp") action { (x, c) =>
        c.copy(timestamp = x)
      } text "timestamp file absolute path"

      opt[String]("lastRunFile") action { (x, c) =>
        c.copy(lastRunFile = x)
      } text "last run file absolute path"

      opt[String]("command") action { (x, c) =>
        c.copy(command = x)
      } text "command to restart the server"

    }
    parser.parse(args, RavenOptions()) map { c =>
      Log.initLog(c.logDir, Log.Verbose)
      new Raven(c.timestamp, c.lastRunFile, c.command.split(" ")).execute()
    } getOrElse {
      println(parser.usage)
      throw new RavenException("invalid args")
    }

  }
}



