/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import org.opalj.br.analyses.SomeProject

import org.apache.commons.text.StringEscapeUtils

/**
 * Trait for representing the config structure
 */
trait ConfigNode {
    var comment: DocumentationComment

    /**
     * Method for handling the export of the configuration structure into an HTML file.
     * @param label required if the Object is part of another object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object. Supply an empty string if not needed.
     * @param pageHTML accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     */
    def toHTML(
        exporter: HTMLExporter,
        label:    String,
        pageHTML: StringBuilder
    )(implicit project: SomeProject): Unit = {
        val labelText = StringEscapeUtils.escapeHtml4(getHeadlineText(label))
        val brief = getBrief(exporter)
        val labelHTML = exporter.headline.replace("$label", if (labelText.isEmpty) brief else labelText)

        val headlineHTML =
            if (labelText.isEmpty) {
                labelHTML.replace("$brief", "")
            } else {
                labelHTML.replace("$brief", exporter.brief.replace("$brief", brief))
            }

        if (this.isInstanceOf[ConfigEntry] && comment.isEmpty) {
            pageHTML ++= headlineHTML.replace("$details", "")
            pageHTML ++= "\n"
        } else {
            pageHTML ++= headlineHTML.replace("$details", exporter.details)
            pageHTML ++= "\n"

            // Write value into HTML code
            val splitContent = exporter.content.split("\\$content")
            pageHTML ++= splitContent(0)
            comment.toHTML(pageHTML)
            entriesToHTML(exporter, pageHTML)
            pageHTML ++= "\n"
            pageHTML ++= splitContent(1)
        }
    }

    /**
     * Returns a text for the HTML headline entry.
     */
    protected def getHeadlineText(label: String): String = {
        if (comment.label.nonEmpty) comment.label
        else label
    }

    /**
     * Returns an HMTL-escaped text for the brief description.
     */
    protected def getBrief(exporter: HTMLExporter)(implicit project: SomeProject): String = {
        if (comment.brief.nonEmpty || comment.description.nonEmpty)
            StringEscapeUtils.escapeHtml4(comment.getBrief(exporter))
        else {
            val brief = new StringBuilder()
            valueToHTML(exporter, brief)
            brief.toString()
        }
    }

    def valueToHTML(exporter: HTMLExporter, pageHTML: StringBuilder)(implicit project: SomeProject): Unit

    /**
     * Produces the HTML for the individual entries.
     * @param pageHTML accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     */
    protected def entriesToHTML(
        exporter: HTMLExporter,
        pageHTML: StringBuilder
    )(implicit project: SomeProject): Unit

    /**
     * Checks if the configNode (and its potential child objects are empty.
     * @return Returns true, if the ConfigNode, its comment and its childObjects are all empty. Returns false otherwise.
     */
    def isEmpty: Boolean

    /**
     * This method expands the current object to represent all objects within the structure.
     * Inverse function of collapse.
     */
    def expand(): Unit

    /**
     * This method collapses the object structure by joining inheriting objects containing only one value.
     * Inverse function of expand (except for comments, which are not unmerged).
     */
    def collapse(): Unit

    /**
     * Method for replacing a potential subclass type in the comment of the Node.
     * @param se Accepts an initialized SubclassExtractor containing the ClassHierarchy required for a successful replacement.
     */
    def replaceClasses(se: SubclassExtractor): Unit
}
