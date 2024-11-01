/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

/**
 * Stores a List structure inside the ConfigNode structure.
 * @param entries contains a List of ConfigNodes.
 * @param comment are all the comments associated with the List.
 */
case class ConfigList(entries: ListBuffer[ConfigNode], comment: DocumentationComment) extends ConfigNode {
    /**
     * Formats the entry into HTML code.
     * @param label required if the Object is part of another object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object. Supply an empty string if not needed.
     * @param HTMLHeadline accepts the HTML syntax of the Headline of the value. Can contain $ label and $ brief flags for filling with content.
     * @param HTMLContent accepts the HTML syntax of the content frame for the value. Must contains a $ content flag for correct rendering.
     * @param sorted accepts a boolean to indicate if the export should sort the keys of the configObjects alphabetically.
     * @param maximumHeadlinePreviewLength accepts an integer that determines the maximum amount of characters that the fallback brief preview can contain.
     * @return returns the Config List as HTML code.
     */
    override def toHTML(
        label:                        String,
        HTMLHeadline:                 String,
        HTMLContent:                  String,
        sorted:                       Boolean,
        maximumHeadlinePreviewLength: Int
    ): String = {
        var HTMLString = ""
        var head = label
        if (comment.label.nonEmpty) head = comment.label

        // Get HTML data for all child Nodes
        var content = "<p>" + comment.toHTML + "</p>\n"
        for (entry <- entries) {
            content += entry.toHTML("", HTMLHeadline, HTMLContent, sorted, maximumHeadlinePreviewLength) + "\n"
        }

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label", head).replace(
            "$brief",
            comment.getBrief(maximumHeadlinePreviewLength)
        ) + "\n"

        // Add content below
        HTMLString += HTMLContent.replace("$content", content) + "\n"

        HTMLString
    }

    /**
     * Checks if the list is empty.
     * @return true if both the List and the comment are empty.
     */
    override def isEmpty: Boolean = {
        if (!comment.isEmpty) return false
        if (entries.forall(entry => !entry.isEmpty)) return false
        true
    }

    /**
     * This method collapses the object structure by joining inheriting objects containing only one value.
     * Inverse function of expand.
     */
    override def collapse(): Unit = {
        entries.foreach(entry => entry.collapse())
    }

    /**
     * This method expands the current object to represent all ob-objects within the structure.
     * Inverse function of collapse.
     */
    override def expand(): Unit = {
        entries.foreach(entry => entry.expand())
    }

    /**
     * Iterator for replacing subclass types of all members of the List.
     * @param se Accepts an initialized SubclassExtractor containing the ClassHierarchy required for a successful replacement.
     */
    override def replaceClasses(se: SubclassExtractor): Unit = {
        entries.foreach(entry => entry.replaceClasses(se))
    }
}
