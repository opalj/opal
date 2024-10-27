/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

/**
 * Trait for representing the config structure
 */
trait ConfigNode {
    val comment: Comment
    def toHTML(
        label:                        String,
        HTMLHeadline:                 String,
        HTMLContent:                  String,
        sorted:                       Boolean,
        maximumHeadlinePreviewLength: Int
    ): String
    def isEmpty: Boolean
    def expand(): Unit
    def collapse(): Unit
    def replaceClasses(se: SubclassExtractor): Unit
}
