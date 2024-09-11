package org.opalj.ce

case class ConfigEntry(value: String, comment: Comment) extends ConfigNode {
  override def toHTML(): String = {
    return ""
  }

  override def commitComments(): Unit = {

  }

}
