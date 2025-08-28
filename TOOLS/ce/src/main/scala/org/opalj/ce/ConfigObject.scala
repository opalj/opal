/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * Stores a List structure inside the ConfigNode structure.
 * @param entries contains a K,V Map of ConfigNodes.
 * @param comment are all the comments associated with the Object.
 */
case class ConfigObject(var entries: mutable.Map[String, ConfigNode], var comment: DocumentationComment)
    extends ConfigNode {
    implicit val logContext: LogContext = GlobalLogContext

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
        def entryToHTML(key: String, entry: ConfigNode): Unit = {
            entry.toHTML(key, headlineHTML, contentHTML, pageHTML, sorted, maximumHeadlinePreviewLength)
            pageHTML ++= "\n"
        }

        if (sorted) {
            val sortedKeys = entries.keys.toSeq.sorted
            for (key <- sortedKeys) {
                entryToHTML(key, entries(key))
            }
        } else {
            for ((key, node) <- entries) {
                entryToHTML(key, node)
            }
        }
    }

    /**
     * Checks if the object is empty.
     * @return true if both the Object and the comment are empty.
     */
    override def isEmpty: Boolean = {
        comment.isEmpty && entries.valuesIterator.forall(_.isEmpty)
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
        for (kvpair @ (key, value) <- insertingObject.entries) {
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
        for ((key, value) <- entries) {
            value.collapse()

            // If the entry is a config object with exactly one child -> merge
            value match {
                case valueObject: ConfigObject if valueObject.entries.size == 1 =>
                    // Merge Keys
                    val (childkey, childvalue) = valueObject.entries.head
                    val newkey = key.trim + "." + childkey.trim

                    // Merge comments
                    childvalue.comment = childvalue.comment.mergeComment(valueObject.comment)

                    // Add new object
                    entries += (newkey -> childvalue)

                    // Remove old object
                    entries -= key
                case _ =>
            }
        }
    }

    /**
     * This method expands the current object to represent all objects within the structure.
     * Inverse function of collapse (except for comments, which are not unmerged).
     */
    def expand(): Unit = {
        for (entry <- entries) {
            // Expand substructure of monitored object
            val (key, value) = entry
            value.expand()

            if (key.contains(".")) {
                // Create expanded object
                val Array(firstKey, remainingKey) = key.split("\\.", 2).map(_.trim)
                val newEntry = mutable.Map[String, ConfigNode](remainingKey -> value)
                val newObject = ConfigObject(newEntry, new DocumentationComment("", "", Seq(), "", Seq()))
                newObject.expand()
                if (entries.contains(firstKey)) {
                    entries(firstKey) match {
                        case configObject: ConfigObject =>
                            configObject.merge(newObject)
                        case other =>
                            // If the child object already exists and is NOT a config object, the config structure has a label conflict (Problem!)
                            throw new IllegalArgumentException(
                                s"Unable to Merge ${firstKey} due to incompatible types: ${other.getClass}"
                            )
                    }
                } else {
                    entries += (firstKey -> newObject)
                }

                // Delete old entry from the map to avoid duplicates
                entries -= key
            }
        }
    }

    /**
     * Replaces subclass types of all members of the Object.
     * @param se Accepts an initialized SubclassExtractor containing the ClassHierarchy required for a successful replacement.
     */
    override def replaceClasses(se: SubclassExtractor): Unit = {
        entries.valuesIterator.foreach(_.replaceClasses(se))
    }
}
