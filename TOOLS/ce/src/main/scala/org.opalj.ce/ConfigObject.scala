package org.opalj.ce

case class ConfigObject(entries: Map[String, ConfigNode], comment: Comment) extends ConfigNode
