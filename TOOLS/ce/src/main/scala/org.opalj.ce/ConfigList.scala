package org.opalj.ce

import scala.collection.mutable.ListBuffer

case class ConfigList(entries: ListBuffer[ConfigNode], comment: Comment) extends ConfigNode {
    override def toHTML(label: String, HTMLHeadline : String, HTMLContent : String): String = {
        var HTMLString = ""
        var head = label
        if(this.comment.label.isEmpty != true) head = this.comment.label

        // Get HTML data for all child Nodes
        var content = "<p>" + comment.toHTML() + "</p>"
        for(entry <- entries){
            content += entry.toHTML("", HTMLHeadline, HTMLContent)
        }

        // Adds Header line with collapse + expand options
        HTMLString += HTMLHeadline.replace("$label",head).replace("$brief",this.comment.brief)

        // Add content below
        HTMLString += HTMLContent.replace("$content", content)

        return HTMLString
    }

    override def isEmpty(): Boolean = {
        for(entry <- entries){
            if(entry.isEmpty() == false) return false
        }
        if(comment.isEmpty() == false) return false
        true
    }
}

