package org.opalj.ce

/**
 * Stores a value inside the structure of the configNode
 * @param value is the value stored in the entry
 * @param comment are all the comments associated with the value
 */
case class ConfigEntry(value: String, comment: Comment) extends ConfigNode {
    /**
     * Formats the entry into HTML code
     * @param label required if the Entry is part of an object (Writes the key of the K,V Map there instead). Overrides the label property of the Comment object.
     * @param HTMLHeadline accepts the HTML syntax of the Headline of the value. Can contain $label and $brief flags for filling with content
     * @param HTMLContent accepts the HTML syntax of the content frame for the value. Must contains a $content flag for correct rendering
     * @return returns the Config Entry as HTML code
     */
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String): String = {
        var HTMLString = ""
        var head = label
        var brief = this.comment.brief
        if(this.comment.label.isEmpty != true) head = this.comment.label
        else if (head == "") head = value

        // Write value into HTML code
        var content = "<b>Value: </b><code>" + value.replace("<","&lt").replace(">","&gt") + "</code><br>"
        content += this.comment.toHTML()

        // If there is no brief preview, put the value into it
        if(comment.isEmpty()){
            brief = "<b>Value: </b><code>" + value.replace("<","&lt").replace(">","&gt") + "</code>"
        }

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label",head).replace("$brief",brief)

        // Add content below
        HTMLString += HTMLContent.replace("$content", content)

        HTMLString
    }

    /**
     * Checks if the value object is empty
     * @return true if both the value and the comment are empty
     */
    override def isEmpty(): Boolean = {
        if(value.isEmpty() && comment.isEmpty()) return true
        false
    }
}
