/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

import org.opalj.br.analyses.SomeProject

import org.apache.commons.text.StringEscapeUtils

/**
 * Stores a List structure inside the ConfigNode structure.
 * @param entries contains a List of ConfigNodes.
 * @param comment are all the comments associated with the List.
 */
case class ConfigList(entries: ListBuffer[ConfigNode], var comment: DocumentationComment) extends ConfigNode {

    /**
     * Produces the HTML for the individual entries.
     * @param pageHTML accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     */
    protected def entriesToHTML(
        exporter: HTMLExporter,
        pageHTML: StringBuilder
    )(implicit project: SomeProject): Unit = {
        for (entry <- entries) {
            entry.createHTML(exporter, "", pageHTML)
            pageHTML ++= "\n"
        }
    }

    override def valueToHTML(exporter: HTMLExporter, pageHTML: StringBuilder)(implicit project: SomeProject): Unit = {
        pageHTML ++= "<b>Value: </b><code> [ "
        val contentHTML = entries.map {
            case e: ConfigEntry  => StringEscapeUtils.escapeHtml4(e.value)
            case _: ConfigObject => "{...}"
            case _: ConfigList   => "[...]"

        }.mkString(", ")
        pageHTML ++= exporter.restrictLength(contentHTML)
        pageHTML ++= " ]</code>"
    }

    /**
     * Checks if the list is empty.
     * @return true if both the List and the comment are empty.
     */
    override def isEmpty: Boolean = {
        comment.isEmpty && entries.forall(_.isEmpty)
    }

    /**
     * This method collapses the object structure by joining inheriting objects containing only one value.
     * Inverse function of expand.
     */
    override def collapse(): Unit = {
        entries.foreach(_.collapse())
    }

    /**
     * This method expands the current object to represent all ob-objects within the structure.
     * Inverse function of collapse (except for comments, which are not unmerged).
     */
    override def expand(): Unit = {
        entries.foreach(_.expand())
    }

    /**
     * Iterator for replacing subclass types of all members of the List.
     * @param se Accepts an initialized SubclassExtractor containing the ClassHierarchy required for a successful replacement.
     */
    override def replaceClasses(se: SubclassExtractor): Unit = {
        entries.foreach(_.replaceClasses(se))
    }
}
