/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

/**
 * Container for the comments of a config node.
 */
class Comment {
    var constraints: ListBuffer[String] = ListBuffer[String]()
    val description: ListBuffer[String] = ListBuffer[String]()
    var label = ""
    var brief = ""
    var datatype = ""

    /**
     * Converts the Comment object into HTML syntax.
     * @return returns the entire object as HTML code ready for insertion into an HTML file.
     */
    def toHTML: String = {
        var HTMLString = ""
        if (!this.isEmpty) {
            if (description.nonEmpty) {
                HTMLString += "<p><b> Description: </b> <br>"
                for (line <- description) {
                    HTMLString += line + "<br>"
                }
                HTMLString += "</p>"
            }
            if (datatype.nonEmpty) HTMLString += "<p><b> Type: </b>" + datatype + "<br></p>"
            if (constraints.nonEmpty) {
                if (datatype.equals("enum")) {
                    HTMLString += "<p><b> Allowed Values: </b><br>"
                } else {
                    HTMLString += "<p><b> Constraints: </b><br>"
                }
                for (line <- constraints) {
                    HTMLString += line + "<br>"
                }
                HTMLString += "</p>"
            }
        }
        HTMLString
    }

    /**
     * Merges another comment into this comment.
     * @param comment accepts the comment that should be merged into this comment.
     */
    def mergeComment(comment: Comment): Unit = {
        // Merge comments
        if (this.label != "" || comment.label != "") {
            this.label = comment.label + "." + this.label
        }
        this.brief = comment.brief + "   " + this.brief
        this.brief = this.brief.trim
        this.description.addAll(comment.description)
        this.constraints.addAll(comment.constraints)
    }

    /**
     * Checks if the comment is empty.
     * @return returns true if the comment is empty but the label property (the label property is set automatically for config files.).
     */
    def isEmpty: Boolean = {
        if (description.isEmpty && constraints.isEmpty && datatype.isEmpty) return true
        false
    }

    def getBrief: String = {
        if (this.brief.isEmpty) {
            if (this.description.nonEmpty) {
                return this.description.head.substring(0, this.description.head.length.min(70)) + "..."
            }
        }
        this.brief
    }

    def replaceClasses(se: SubclassExtractor): Unit = {
        if (this.datatype.trim.equals("subclass")) {
            // Get a Set of all subclasses
            val root = this.constraints.head

            // Replace Types
            this.datatype = "enum"
            this.constraints = ListBuffer(se.extractSubclasses(root).toSeq: _*)
        }
    }
    /**
     * Prints the Comment object to the console
     * Debug purposes
     */
    def printObject(): Unit = {
        println("Constraints: " + constraints.toString())
        println("Description: " + description.toString())
        println("Label: " + label)
        println("Brief: " + brief)
        println("Type: " + datatype)
    }
}

object Comment {
    /**
     *  Factory method for creating a comment.
     *  @param commentBuffer accepts a ListBuffer that contains the raw content of the comment
     *  @return is a fully functional Comment
     */
    def fromString(commentBuffer: ListBuffer[String]): Comment = {
        val comment = new Comment()
        for (line <- commentBuffer) {
            println(line)
            if (line.trim.startsWith("@label")) {
                comment.label = line.trim.stripPrefix("@label").trim
            } else if (line.trim.startsWith("@brief")) {
                comment.brief = line.trim.stripPrefix("@brief").trim
            } else if (line.trim.startsWith("@constraint")) {
                comment.constraints += line.trim.stripPrefix("@constraint").trim
            } else if (line.trim.startsWith("@type")) {
                comment.datatype = line.trim.stripPrefix("@type").trim
            } else {
                comment.description += line.trim.stripPrefix("@description").trim
            }
        }
        comment
    }
}
