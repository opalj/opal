package org.opalj.ce

trait ConfigNode {
    def toHTML() : String
    def commitComments() : Unit
}
