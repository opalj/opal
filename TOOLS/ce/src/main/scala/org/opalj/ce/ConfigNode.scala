/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import org.apache.commons.text.StringEscapeUtils

/**
 * Trait for representing the config structure
 */
trait ConfigNode {
    var comment: DocumentationComment

    /**
     * Method for handling the export of the configuration structure into an HTML file.
     * @param label required if the Object is part of another object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object. Supply an empty string if not needed.
     * @param headlineHTML accepts the HTML syntax of the Headline of the value. Can contain $label and $brief flags for filling with content.
     * @param contentHTML accepts the HTML syntax of the content frame for the value. Must contain a $content flag for correct rendering.
     * @param pageHTML accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     * @param sorted accepts a boolean to indicate if the export should sort the keys of the configObjects alphabetically.
     * @param maximumHeadlinePreviewLength accepts an integer that determines the maximum amount of characters that the fallback brief preview can contain.
     */
    def toHTML(
        label:                        String,
        headlineHTML:                 String,
        contentHTML:                  String,
        pageHTML:                     StringBuilder,
        sorted:                       Boolean,
        maximumHeadlinePreviewLength: Int
    ): Unit = {
        val head = getHeadlineText(label)

        val brief = getBrief(maximumHeadlinePreviewLength)

        // Adds Header line with collapse + expand options
        pageHTML ++= headlineHTML.replace("$label", StringEscapeUtils.escapeHtml4(head)).replace("$brief", brief)
        pageHTML ++= "\n"

        // Write value into HTML code
        val splitContent = contentHTML.split("\\$content")
        pageHTML ++= splitContent(0)
        comment.toHTML(pageHTML)
        entriesToHTML(headlineHTML, contentHTML, pageHTML, sorted, maximumHeadlinePreviewLength)
        pageHTML ++= "<br>\n"
        pageHTML ++= splitContent(1)
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
    protected def getBrief(maximumHeadlinePreviewLength: Int): String = {
        StringEscapeUtils.escapeHtml4(comment.getBrief(maximumHeadlinePreviewLength))
    }

    /**
     * Produces the HTML for the individual entries.
     * @param headlineHTML accepts the HTML syntax of the Headline of the value. Can contain $ label and $ brief flags for filling with content.
     * @param contentHTML accepts the HTML syntax of the content frame for the value. Must contains a $ content flag for correct rendering.
     * @param pageHTML accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     * @param sorted accepts a boolean to indicate if the export should sort the keys of the configObjects alphabetically.
     * @param maximumHeadlinePreviewLength accepts an integer that determines the maximum amount of characters that the fallback brief preview can contain.
     */
    protected def entriesToHTML(
        headlineHTML:                 String,
        contentHTML:                  String,
        pageHTML:                     StringBuilder,
        sorted:                       Boolean,
        maximumHeadlinePreviewLength: Int
    ): Unit

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
