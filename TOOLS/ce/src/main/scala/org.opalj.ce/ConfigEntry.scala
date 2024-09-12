package org.opalj.ce

case class ConfigEntry(value: String, comment: Comment) extends ConfigNode {
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String): String = {
        var HTMLString = comment.toHTML()

        // Placeholder: The Comment object is supposed to store the label later and it should be retrieved from there
        var head = label
        if(head == "") head = value

        // Get HTML data for all child Nodes
        var content = "Value: " + value + "<br>"
        content += comment.toHTML()

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label",head)

        // Add content below
        HTMLString += HTMLContent.replace("$Content", "Value = " + this.value)

        return HTMLString
    }
}
