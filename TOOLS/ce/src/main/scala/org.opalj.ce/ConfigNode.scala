package org.opalj.ce

trait ConfigNode {
    def toHTML(label : String, HTMLHeadline: String, HTMLContent: String): String
    def isEmpty(): Boolean
}
