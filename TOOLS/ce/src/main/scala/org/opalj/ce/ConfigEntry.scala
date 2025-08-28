/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import org.apache.commons.text.StringEscapeUtils

/**
 * Stores a value inside the structure of the configNode.
 *
 * @param value is the value stored in the entry.
 * @param comment are all the comments associated with the value.
 */
case class ConfigEntry(value: String, var comment: DocumentationComment) extends ConfigNode {

    /**
     * Returns a text for the HTML headline entry.
     */
    override protected def getHeadlineText(label: String): String = {
        if (comment.label.nonEmpty) comment.label
        else if (label.nonEmpty) label
        else value
    }

    /**
     * Returns an HMTL-escaped text for the brief description.
     */
    override protected def getBrief(maximumHeadlinePreviewLength: Int): String = {
        if (comment.brief.isEmpty) {
            s"<b>Value: </b><code> ${StringEscapeUtils.escapeHtml4(value)} </code>\n"
        } else {
            StringEscapeUtils.escapeHtml4(comment.brief)
        }
    }

    /**
     * Produces the HTML for the individual entries.
     * @param headlineHTML                 accepts the HTML syntax of the Headline of the value. Can contain $label and $brief flags for filling with content.
     * @param contentHTML                  accepts the HTML syntax of the content frame for the value. Must contain a $content flag for correct rendering.
     * @param pageHTML                     accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     * @param sorted                       accepts a boolean to indicate if the export should sort the keys of the configObjects alphabetically.
     * @param maximumHeadlinePreviewLength accepts an integer that determines the maximum amount of characters that the fallback brief preview can contain.
     */
    override protected def entriesToHTML(
        headlineHTML:                 String,
        contentHTML:                  String,
        pageHTML:                     StringBuilder,
        sorted:                       Boolean,
        maximumHeadlinePreviewLength: UByte
    ): Unit = {
        pageHTML ++= "<b>Value: </b><code> "
        pageHTML ++= StringEscapeUtils.escapeHtml4(value)
        pageHTML ++= "</code>"
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
