package org.opalj
package ce

/**
 * Trait for representing the config structure
 * Provides a Hull that the inheriting classes have to implement to function
 */
trait ConfigNode {
    val comment : Comment
    def toHTML(label : String, HTMLHeadline: String, HTMLContent: String, sorted : Boolean): String
    def isEmpty(): Boolean
    def expand() : Unit
    def collapse() : Unit
    def replaceClasses(se : SubclassExtractor): Unit
}
