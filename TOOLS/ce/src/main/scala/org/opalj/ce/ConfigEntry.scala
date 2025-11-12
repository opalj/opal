/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import org.opalj.br.ClassType
import org.opalj.br.analyses.SomeProject

import org.apache.commons.text.StringEscapeUtils

/**
 * Stores a value inside the structure of the configNode.
 *
 * @param value is the value stored in the entry.
 * @param comment are all the comments associated with the value.
 */
case class ConfigEntry(value: String, var comment: DocumentationComment) extends ConfigNode {

    override def valueToHTML(exporter: HTMLExporter, pageHTML: StringBuilder)(implicit project: SomeProject): Unit = {
        pageHTML ++= "<b>Value: </b><code> "
        val escapedVal = StringEscapeUtils.escapeHtml4(value)
        if (value.startsWith("org.opalj.")) {
            if (project.classFile(ClassType(value.replace('.', '/') + "$")).isDefined) {
                pageHTML ++= s"<a href=\"${escapedVal.replace('.', '/')}$$.html\">$escapedVal</a>"
            } else if (project.classFile(ClassType(value.replace('.', '/'))).isDefined) {
                pageHTML ++= s"<a href=\"${escapedVal.replace('.', '/')}.html\">$escapedVal</a>"
            } else {
                pageHTML ++= escapedVal
            }
        } else {
            pageHTML ++= escapedVal
        }
        pageHTML ++= "</code>"
    }

    /**
     * Produces the HTML for the individual entries.
     * @param pageHTML                     accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     */
    override protected def entriesToHTML(
        exporter: HTMLExporter,
        pageHTML: StringBuilder
    )(implicit project: SomeProject): Unit = {
        valueToHTML(exporter, pageHTML)
    }

    /**
     * Checks if the value object is empty.
     * @return true if both the value and the comment are empty.
     */
    override def isEmpty: Boolean = {
        comment.isEmpty && value.isEmpty
    }

    /**
     * Collapse is not needed in config Entry, due to it not having any sub-objects.
     */
    override def collapse(): Unit = {}

    /**
     * Expand is not needed in config Entry, due to it not having any sub-objects.
     */
    override def expand(): Unit = {}

    /**
     * Method for replacing a potential subclass type in the comment of the entry.
     * @param se Accepts an initialized SubclassExtractor containing the ClassHierarchy required for a successful replacement.
     */
    override def replaceClasses(se: SubclassExtractor): Unit = {
        comment = comment.replaceClasses(se)
    }
}
