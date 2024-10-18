/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

/**
 * Stores a List structure inside the ConfigNode structure
 * @param entries contains a List of ConfigNodes
 * @param comment are all the comments associated with the List
 */
case class ConfigList(entries: ListBuffer[ConfigNode], comment: Comment) extends ConfigNode {
    /**
     * Formats the entry into HTML code
     * @param label required if the List is part of an object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object.
     * @param HTMLHeadline accepts the HTML syntax of the Headline of the value. Can contain $ label and $ brief flags for filling with content.
     * @param HTMLContent accepts the HTML syntax of the content frame for the value. Must contains a $ content flag for correct rendering.
     * @return returns the Config List as HTML code
     */
    override def toHTML(label: String, HTMLHeadline: String, HTMLContent: String, sorted: Boolean): String = {
        var HTMLString = ""
        var head = label
        if (this.comment.label.nonEmpty) head = this.comment.label

        // Get HTML data for all child Nodes
        var content = "<p>" + comment.toHTML + "</p>"
        for (entry <- entries) {
            content += entry.toHTML("", HTMLHeadline, HTMLContent, sorted)
        }

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label", head).replace("$brief", this.comment.getBrief)

        // Add content below
        HTMLString += HTMLContent.replace("$content", content)

        HTMLString
    }

    /**
     * Checks if the list is empty
     * @return true if both the List and the comment are empty
     */
    override def isEmpty: Boolean = {
        for (entry <- entries) {
            if (!entry.isEmpty) return false
        }
        if (!comment.isEmpty) return false
        true
    }

    /**
     * This method collapses the object structure by joining inheriting objects containing only one value
     * Inverse function of expand
     */
    override def collapse(): Unit = {
        for (entry <- this.entries) {
            entry.collapse()
        }
    }

    /**
     * This method expands the current object to represent all ob-objects within the structure
     * Inverse function of collapse
     */
    override def expand(): Unit = {
        for (entry <- this.entries) {
            entry.expand()
        }
    }

    override def replaceClasses(se: SubclassExtractor): Unit = {
        for (entry <- entries) {
            entry.replaceClasses(se)
        }
    }
}
