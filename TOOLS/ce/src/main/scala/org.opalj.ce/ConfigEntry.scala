package org.opalj.ce

case class ConfigEntry(value: String, comment: Comment) extends ConfigNode {
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String): String = {
        var HTMLString = ""

        // Placeholder: The Comment object is supposed to store the label later and it should be retrieved from there
        var head = label
        if(this.comment.label.isEmpty != true) head = this.comment.label
        else if (head == "") head = value

        // Get HTML data for all child Nodes
        var content = "<b>Value: </b>" + value + "<br>"
        content += this.comment.toHTML()

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label",head).replace("$brief",this.comment.brief)

        // Add content below
        HTMLString += HTMLContent.replace("$content", content)

        return HTMLString
    }
}
