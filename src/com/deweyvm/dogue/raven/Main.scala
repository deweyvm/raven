package com.deweyvm.dogue.raven

import com.deweyvm.dogue.common.logging.Log

case class RavenOptions(logDir:String=".", timestamp:String=".", lastRunFile:String=".", command:Seq[String]=Seq())

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

      arg[String]("command") unbounded() action { (x, c) =>
        c.copy(command = c.command :+ x)
      } text "command to restart the server"

    }
    parser.parse(args, RavenOptions()) map { c =>
      Log.setDirectory(c.logDir)
        new Raven(c.timestamp, c.lastRunFile, c.command)
    } getOrElse {
      println(parser.usage)
      throw new RavenException("invalid args")
    }

  }
}



