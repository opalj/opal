package org.opalj.ce

import scala.collection.mutable.ListBuffer

class Comment {
    val commentBuffer: ListBuffer[String] = ListBuffer[String]()
    val constraints : ListBuffer[String] = ListBuffer[String]()
    val description : ListBuffer[String] = ListBuffer[String]()
    var label = ""
    var brief = ""
    var datatype = ""

    // Adds a comment to the comment buffer. If a comment has multiple lines, this method will be called multiple times
    def addComment(comment: String): Unit = {
        this.commentBuffer += comment
    }

    // Tells the Comment object that no more comments will be added. The comment object then begins parsing the comments into advanced objects
    def commitComments(): Unit = {
        for(line <- commentBuffer){
            if(line.trim.startsWith("@label")) {
                this.label = line.trim.stripPrefix("@label").trim
            } else if(line.trim.startsWith("@brief")) {
                brief = line.trim.stripPrefix("@brief").trim
            } else if(line.trim.startsWith("@constraint")){
                constraints.addOne(line.trim.stripPrefix("@constraint").trim)
            } else if(line.trim.startsWith("@type")){
                datatype = line.trim.stripPrefix("@type").trim
            } else {
                description.addOne(line.trim.stripPrefix("@description").trim)
            }
        }
    }

    def toHTML(): String = {
        var HTMLString = ""
        if(isEmpty() == false) {
            HTMLString += "<p>"
            if(description.length > 0) {
                HTMLString += "<b> Description: </b> <br>"
                for (line <- description) {
                    HTMLString += line + "<br>"
                }
                HTMLString += "<br> <br>"
            }
            if(datatype.length > 0) HTMLString += "<b> Type: </b>" + datatype + "<br>"
            if(constraints.length > 0) {
                HTMLString += "<b> Constraints: </b><br>"
                for (line <- constraints) {
                    HTMLString += line + "<br>"
                }
            }
            HTMLString += "</p>"
        }
        return HTMLString
    }

    def isEmpty(): Boolean = {
        if(commentBuffer.length <= 0) return true
        if(commentBuffer.length <= 1 && label.trim != "") return true
        false
    }

    def printObject() : Unit = {
        println("CommentBuffer: " + commentBuffer.toString())
        println("Constraints: " + constraints.toString())
        println("Description: " + description.toString())
        println("Label: " + label)
        println("Brief: " + brief)
        println("Type: " + datatype)
    }
}
