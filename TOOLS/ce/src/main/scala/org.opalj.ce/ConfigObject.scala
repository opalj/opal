package org.opalj.ce

case class ConfigObject(entries: Map[String, ConfigNode], comment: Comment) extends ConfigNode {
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String): String = {
        var HTMLString = ""

        // Placeholder: The Comment object is supposed to store the label later and it should be retrieved from there
        var head = label
        if(head == "") head = "placeholder"

        // Get HTML data for all child Nodes
        var content = "<p>" + comment.toHTML() + "</p>"
        for((key,node) <- entries){
            content += node.toHTML(key, HTMLHeadline, HTMLContent)
        }
        content += "<hr>"

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label",head)

        // Add content below
        HTMLString += HTMLContent.replace("$Content", content)

        return HTMLString
    }
}