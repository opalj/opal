package org.opalj
package ce

/**
 * Trait for representing the config structure
 * Provides a Hull that the inheriting classes have to implement to function
 */
trait ConfigNode {
    def toHTML(label : String, HTMLHeadline: String, HTMLContent: String): String
    def isEmpty(): Boolean
}
