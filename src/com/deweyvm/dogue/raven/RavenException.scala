package com.deweyvm.dogue.raven

case class RavenException(msg:String) extends RuntimeException(msg)
