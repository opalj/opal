package org.opalj.ce

case class ConfigEntry(value: String, comment: Comment) extends ConfigNode {
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String): String = {
        var HTMLString = ""
        var head = label
        var brief = this.comment.brief
        if(this.comment.label.isEmpty != true) head = this.comment.label
        else if (head == "") head = value

        // Get HTML data for all child Nodes
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

    override def isEmpty(): Boolean = {
        if(value.isEmpty() && comment.isEmpty()) return true
        false
    }
}
