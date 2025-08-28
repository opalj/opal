/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

/**
 * Stores a List structure inside the ConfigNode structure.
 * @param entries contains a List of ConfigNodes.
 * @param comment are all the comments associated with the List.
 */
case class ConfigList(entries: ListBuffer[ConfigNode], var comment: DocumentationComment) extends ConfigNode {

    /**
     * Produces the HTML for the individual entries.
     * @param headlineHTML accepts the HTML syntax of the Headline of the value. Can contain $label and $brief flags for filling with content.
     * @param contentHTML accepts the HTML syntax of the content frame for the value. Must contain a $content flag for correct rendering.
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
    ): Unit = {
        for (entry <- entries) {
            entry.toHTML("", headlineHTML, contentHTML, pageHTML, sorted, maximumHeadlinePreviewLength)
            pageHTML ++= "\n"
        }
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
