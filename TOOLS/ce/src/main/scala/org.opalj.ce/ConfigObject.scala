/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

import org.apache.commons.text.StringEscapeUtils

/**
 * Stores a List structure inside the ConfigNode structure.
 * @param entries contains a K,V Map of ConfigNodes.
 * @param comment are all the comments associated with the Object.
 */
case class ConfigObject(var entries: mutable.Map[String, ConfigNode], var comment: DocumentationComment)
    extends ConfigNode {
    implicit val logContext: LogContext = GlobalLogContext
    /**
     * Formats the entry into HTML code.
     * @param label required if the Object is part of another object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object. Supply an empty string if not needed.
     * @param HTMLHeadline accepts the HTML syntax of the Headline of the value. Can contain $ label and $ brief flags for filling with content.
     * @param HTMLContent accepts the HTML syntax of the content frame for the value. Must contains a $ content flag for correct rendering.
     * @param HTMLStringBuilder accepts a StringBuilder. The method adds the HTML String to this StringBuilder.
     * @param sorted accepts a boolean to indicate if the export should sort the keys of the configObjects alphabetically.
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
        val head = if (comment.label.nonEmpty) {
            comment.label
        } else {
            label
        }

        val brief = comment.getBrief(maximumHeadlinePreviewLength)

        // Adds Header line with collapse + expand options
        HTMLStringBuilder ++= HTMLHeadline.replace("$label", StringEscapeUtils.escapeHtml4(head)).replace(
            "$brief",
            StringEscapeUtils.escapeHtml4(brief)
        )
        HTMLStringBuilder ++= "\n"

        // Write value into HTML code
        val splitContent = HTMLContent.split("\\$content")
        HTMLStringBuilder ++= splitContent(0)
        comment.toHTML(HTMLStringBuilder)
        if (sorted) {
            val sortedKeys = entries.keys.toSeq.sorted
            for (key <- sortedKeys) {
                entries(key).toHTML(
                    key,
                    HTMLHeadline,
                    HTMLContent,
                    HTMLStringBuilder,
                    sorted,
                    maximumHeadlinePreviewLength
                )
                HTMLStringBuilder ++= "\n"
            }
        } else {
            for ((key, node) <- entries) {
                node.toHTML(key, HTMLHeadline, HTMLContent, HTMLStringBuilder, sorted, maximumHeadlinePreviewLength)
                HTMLStringBuilder ++= "\n"
            }
        }
        HTMLStringBuilder ++= "<br>\n"
        HTMLStringBuilder ++= splitContent(1)

        HTMLStringBuilder.toString
    }

    /**
     * Checks if the object is empty.
     * @return true if both the Object and the comment are empty.
     */
    override def isEmpty: Boolean = {
        if (!comment.isEmpty) return false
        for ((key, value) <- entries) {
            if (!value.isEmpty) return false
        }
        true
    }

    /**
     * Merges two type compatible objects.
     * This means that the objects are free of conflicting values and lists. Objects are allowed to overlap as long as there are no conflicts down the tree.
     * @param insertingObject Is the object that is supposed to be merged into the executing one.
     */
    def merge(insertingObject: ConfigObject): Unit = {

        // Expanding both objects guarantees compatible key naming syntax
        expand()
        insertingObject.expand()

        // Insert object
        for (kvpair <- insertingObject.entries) {
            val (key, value) = kvpair
            if (entries.contains(key)) {
                val conflicting_entry = entries.getOrElse(key, null)
                if (conflicting_entry.isInstanceOf[ConfigObject] && value.isInstanceOf[ConfigObject]) {
                    val conflicting_child_object = conflicting_entry.asInstanceOf[ConfigObject]
                    conflicting_child_object.merge(value.asInstanceOf[ConfigObject])
                } else {
                    OPALLogger.error("Configuration Explorer", s"Info on incompatible keys: ${key.trim}")
                    throw new IllegalArgumentException(
                        s"Unable to merge incompatible types: ${value.getClass} & ${conflicting_entry.getClass}"
                    )
                }
            } else {
                OPALLogger.info("Configuration Explorer", s"No conflict detected. Inserting ${key.trim}")
                entries += kvpair
            }
        }

        collapse()
    }

    /**
     * This method collapses the object structure by joining inheriting objects containing only one value.
     * Inverse function of expand.
     */
    def collapse(): Unit = {
        for (entry <- entries) {
            val (key, value) = entry
            value.collapse()

            // If the entry is a config object with exactly one child -> merge
            if (value.isInstanceOf[ConfigObject]) {
                val value_object = value.asInstanceOf[ConfigObject]
                if (value_object.entries.size == 1) {
                    // Merge Keys
                    val (childkey, childvalue) = value_object.entries.head
                    val newkey = key.trim + "." + childkey.trim

                    // Merge comments
                    childvalue.comment = childvalue.comment.mergeComment(value_object.comment)

                    // Add new object
                    entries += (newkey -> childvalue)

                    // Remove old object
                    entries -= key
                }
            }
        }
        if (entries.size == 1) {
            if (comment.isEmpty) {} else {
                val (key, value) = entries.head
                if (value.comment.isEmpty) {}
            }
        }
    }

    /**
     * This method expands the current object to represent all objects within the structure.
     * Inverse function of collapse.
     */
    def expand(): Unit = {
        for (entry <- entries) {
            // Expand substructure of monitored object
            val (key, value) = entry
            value.expand()

            if (key.contains(".")) {
                // Create expanded object
                val newkey = key.trim.split("\\.", 2)
                val new_entry = mutable.Map[String, ConfigNode](newkey(1).trim -> value)
                val new_object = ConfigObject(new_entry, new DocumentationComment("", "", Seq(), "", Seq()))
                new_object.expand()
                if (entries.contains(newkey(0).trim)) {
                    if (entries(newkey(0).trim).isInstanceOf[ConfigObject]) {
                        entries(newkey(0).trim).asInstanceOf[ConfigObject].merge(new_object)
                    } else {
                        // If the child object already exists and is NOT a config object, the config structure has a label conflict (Problem!)
                        throw new IllegalArgumentException(
                            s"Unable to Merge ${newkey(0).trim} due to incompatible types: ${entries(newkey(0).trim).getClass}"
                        )
                    }
                } else {
                    entries += (newkey(0).trim -> new_object)
                }

                // Delete old entry from the map to avoid duplicates
                entries -= key
            }
        }
    }

    /**
     * Iterator for replacing subclass types of all members of the Object.
     * @param se Accepts an initialized SubclassExtractor containing the ClassHierarchy required for a successful replacement.
     */
    override def replaceClasses(se: SubclassExtractor): Unit = {
        for ((key, value) <- entries) {
            value.replaceClasses(se)
        }
    }
}
