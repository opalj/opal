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
     * Formats the entry into HTML code.
     * @param label required if the Entry is part of an object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object.
     * @param HTMLHeadline accepts the HTML syntax of the Headline of the value. Can contain $ label and $ brief flags for filling with content.
     * @param HTMLContent accepts the HTML syntax of the content frame for the value. Must contains a $ content flag for correct rendering.
     * @param HTMLStringBuilder accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     * @param sorted actually does nothing in this method, but is required for the whole structure.
     * @param maximumHeadlinePreviewLength accepts an integer that determines the maximum amount of characters that the fallback brief preview can contain.
     */
    override def toHTML(
        label:                        String,
        HTMLHeadline:                 String,
        HTMLContent:                  String,
        HTMLStringBuilder:            StringBuilder,
        sorted:                       Boolean,
        maximumHeadlinePreviewLength: Int
    ): Unit = {
        // Set headline text
        val head =
            if (comment.label.nonEmpty) comment.label
            else if (label.nonEmpty) label
            else value

        // If there is no brief preview, put the value into it
        val brief = if (comment.brief.isEmpty) {
            s"<b>Value: </b><code> ${StringEscapeUtils.escapeHtml4(value)} </code>\n"
        } else {
            StringEscapeUtils.escapeHtml4(comment.brief)
        }

        // Adds Header line with collapse + expand options
        HTMLStringBuilder ++= HTMLHeadline.replace("$label", StringEscapeUtils.escapeHtml4(head)).replace(
            "$brief",
            brief
        )
        HTMLStringBuilder ++= "\n"

        // Write value into HTML code
        val splitContent = HTMLContent.split("\\$content")
        HTMLStringBuilder ++= splitContent(0)
        comment.toHTML(HTMLStringBuilder)
        HTMLStringBuilder ++= "<b>Value: </b><code> "
        HTMLStringBuilder ++= StringEscapeUtils.escapeHtml4(value)
        HTMLStringBuilder ++= "</code><br>\n"
        HTMLStringBuilder ++= splitContent(1)
    }

    /**
     * Checks if the value object is empty.
     * @return true if both the value and the comment are empty.
     */
    override def isEmpty: Boolean = {
        if (value.isEmpty && comment.isEmpty) return true
        false
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
