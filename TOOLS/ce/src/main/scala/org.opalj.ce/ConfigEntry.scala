package org.opalj.ce

case class ConfigEntry(value: String, comment: Comment) extends ConfigNode {
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String): String = {
        var HTMLString = ""
        var head = label
        if(this.comment.label.isEmpty != true) head = this.comment.label
        else if (head == "") head = value

        // Get HTML data for all child Nodes
        var content = "<b>Value: </b><code>" + value.replace("<","&lt").replace(">","&gt") + "</code><br>"
        content += this.comment.toHTML()

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label",head).replace("$brief",this.comment.brief)

        // Add content below
        HTMLString += HTMLContent.replace("$content", content)

        return HTMLString
    }

    override def isEmpty(): Boolean = {
        if(value.isEmpty() && comment.isEmpty()) return true
        false
    }
}
