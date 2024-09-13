package org.opalj.ce

/**
 * Stores a List structure inside the ConfigNode structure
 * @param entries contains a K,V Map of ConfigNodes
 * @param comment are all the comments associated with the Object
 */
case class ConfigObject(entries: Map[String, ConfigNode], comment: Comment) extends ConfigNode {
    /**
     * Formats the entry into HTML code
     * @param label required if the Object is part of another object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object.
     * @param HTMLHeadline accepts the HTML syntax of the Headline of the value. Can contain $label and $brief flags for filling with content
     * @param HTMLContent accepts the HTML syntax of the content frame for the value. Must contains a $content flag for correct rendering
     * @return returns the Config Object as HTML code
     */
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String): String = {
        var HTMLString = ""
        var head = label
        if(this.comment.label.isEmpty == false) head = this.comment.label

        // Get HTML data for all child Nodes
        var content = "<p>" + comment.toHTML() + "</p>"
        for((key,node) <- entries){
            content += node.toHTML(key, HTMLHeadline, HTMLContent)
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
        if(comment.isEmpty() == false) return false
        for((key,value) <- entries){
            if(value.isEmpty() == false) return false
        }
        true
    }
}