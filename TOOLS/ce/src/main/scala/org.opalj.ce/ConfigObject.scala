package org.opalj
package ce

import scala.collection.mutable

/**
 * Stores a List structure inside the ConfigNode structure
 *
 * @param entries contains a K,V Map of ConfigNodes
 * @param comment are all the comments associated with the Object
 */
case class ConfigObject(var entries: mutable.Map[String, ConfigNode], comment: Comment) extends ConfigNode {
    /**
     * Formats the entry into HTML code
     * @param label required if the Object is part of another object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object.
     * @param HTMLHeadline accepts the HTML syntax of the Headline of the value. Can contain $label and $brief flags for filling with content
     * @param HTMLContent accepts the HTML syntax of the content frame for the value. Must contains a $content flag for correct rendering
     * @return returns the Config Object as HTML code
     */
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String, sorted: Boolean): String = {
        var HTMLString = ""
        var head = label
        if(!this.comment.label.isEmpty) head = this.comment.label

        // Get HTML data for all child Nodes
        var content = "<p>" + comment.toHTML() + "</p>"

        if(sorted){
            val sortedKeys = entries.keys.toSeq.sorted

            for(key <- sortedKeys){
                content += entries(key).toHTML(key, HTMLHeadline, HTMLContent, sorted)
            }
        } else {
            for ((key, node) <- entries) {
                content += node.toHTML(key, HTMLHeadline, HTMLContent, sorted)
            }
        }

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label",head).replace("$brief",this.comment.brief)

        // Add content below
        HTMLString += HTMLContent.replace("$content", content)

        HTMLString
    }

    /**
     * Checks if the object is empty
     * @return true if both the Object and the comment are empty
     */
    override def isEmpty(): Boolean = {
        if(!comment.isEmpty()) return false
        for((key,value) <- entries){
            if(!value.isEmpty()) return false
        }
        true
    }

    /**
     * Merges two type compatible objects.
     * This means that the objects are free of conflicting values and lists. Objects are allowed to overlap as long as there are no conflicts down the tree.
     * @param insertingObject Is the object that is supposed to be merged into the executing one
     */
    def merge(insertingObject : ConfigObject): Unit = {

        // Expanding both objects guarantees compatible key naming syntax
        this.expand()
        insertingObject.expand()

        // Insert object
        for(kvpair <- insertingObject.entries){
            val (key,value) = kvpair
            if(this.entries.contains(key)){
                val conflicting_entry = this.entries.getOrElse(key,null)
                if(conflicting_entry.isInstanceOf[ConfigObject] && value.isInstanceOf[ConfigObject]){
                    val conflicting_child_object = conflicting_entry.asInstanceOf[ConfigObject]
                    conflicting_child_object.merge(value.asInstanceOf[ConfigObject])
                } else {
                    println("Info on incompatible keys: " + key.trim)
                    throw new Exception("Unable to merge incompatible types:" + value.getClass + " & " + conflicting_entry.getClass)
                }
            } else {
                println("No conflict detected. Inserting " + key.trim)
                this.entries += kvpair
            }
        }

        this.collapse()
    }

    /**
     * This method collapses the object structure by joining inheriting objects containing only one value.
     * Inverse function of expand.
     */
    def collapse() : Unit = {
        for(entry <- this.entries){
            val (key,value) = entry
            value.collapse()

            // If the entry is a config object with exactly one child -> merge
            if(value.isInstanceOf[ConfigObject]){
                val value_object = value.asInstanceOf[ConfigObject]
                if(value_object.entries.size == 1){
                    // Merge Keys
                    val (childkey,childvalue) = value_object.entries.head
                    val newkey = key.trim + "." + childkey.trim

                    // Merge comments
                    childvalue.comment.mergeComment(value_object.comment)

                    // Add new object
                    this.entries += (newkey -> childvalue)

                    // Remove old object
                    this.entries -= key
                }
            }
        }
        if(this.entries.size == 1){
            if(this.comment.isEmpty()){

            } else {
                val (key, value) = this.entries.head
                if(value.comment.isEmpty()){

                }
            }
        }
    }

    /**
     * This method expands the current object to represent all ob-objects within the structure
     * Inverse function of collapse
     */
    def expand() : Unit = {
        for(entry <- this.entries){
            // Expand substructure of monitored object
            val (key,value) = entry
            value.expand()

            if(key.contains(".")) {
                // Create expanded object
                val newkey = key.trim.split("\\.",2)
                val new_entry = mutable.Map[String, ConfigNode](newkey(1).trim -> value)
                val new_object = ConfigObject(new_entry, new Comment)
                new_object.expand()
                if(this.entries.contains(newkey(0).trim)) {
                    if(this.entries(newkey(0).trim).isInstanceOf[ConfigObject]) {
                        this.entries(newkey(0).trim).asInstanceOf[ConfigObject].merge(new_object)
                    } else {
                        throw new Exception("Unable to Merge " + newkey(0).trim + "due to incompatible types: " + this.entries(newkey(0).trim).getClass)
                    }
                } else {
                    this.entries += (newkey(0).trim -> new_object)
                }

                // Delete old entry from the map to avoid duplicates
                this.entries -= key
            }
        }
    }
}