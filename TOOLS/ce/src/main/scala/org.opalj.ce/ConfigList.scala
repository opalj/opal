package org.opalj.ce

import scala.collection.mutable.ListBuffer

case class ConfigList(entries: ListBuffer[ConfigNode], comment: Comment) extends ConfigNode {
  override def toHTML(): String = {
      return ""
  }

  override def commitComments(): Unit = {

  }
}

