/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

/**
 * Container for the comments of a config node.
 */
class Comment {
    val commentBuffer: ListBuffer[String] = ListBuffer[String]()
    var constraints: ListBuffer[String] = ListBuffer[String]()
    val description: ListBuffer[String] = ListBuffer[String]()
    var label = ""
    var brief = ""
    var datatype = ""
    var commentsCommitted = false

    /**
     * Adds a comment to the comment buffer. If a comment has multiple lines, this method will be called multiple times.
     * If the Comment is already commited, nothing happens.
     * @param comment is the string that is added to the comment.
     */
    def addComment(comment: String): Unit = {
        if (!commentsCommitted) this.commentBuffer += comment
    }

    // Tells the Comment object that no more comments will be added. The comment object then begins parsing the comments into advanced objects

    /**
     * Starts parsing the flags within the comment. Can only be called once.
     * After it was called, no new comments can be added.
     */
    def commitComments(): Unit = {
        if (!commentsCommitted) {
            for (line <- commentBuffer) {
                if (line.trim.startsWith("@label")) {
                    this.label = line.trim.stripPrefix("@label").trim
                } else if (line.trim.startsWith("@brief")) {
                    brief = line.trim.stripPrefix("@brief").trim
                } else if (line.trim.startsWith("@constraint")) {
                    constraints.addOne(line.trim.stripPrefix("@constraint").trim)
                } else if (line.trim.startsWith("@type")) {
                    datatype = line.trim.stripPrefix("@type").trim
                } else {
                    description.addOne(line.trim.stripPrefix("@description").trim)
                }
            }
            commentsCommitted = true
        }
    }

    /**
     * Converts the Comment object into HTML syntax.
     * Commits the Comments if it has not happened previously.
     * @return returns the entire object as HTML code ready for insertion into an HTML file.
     */
    def toHTML: String = {
        if (!commentsCommitted) commitComments()
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
     * Merges another committed comment into this comment.
     * @param comment accepts the comment that should be merged into this comment.
     */
    def mergeComment(comment: Comment): Unit = {
        // Make sure every comment is committed
        this.commitComments()
        comment.commitComments()

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
        if (commentBuffer.length <= 0) return true
        if (commentBuffer.length <= 1 && label.trim != "") return true
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
        println("CommentBuffer: " + commentBuffer.toString())
        println("Constraints: " + constraints.toString())
        println("Description: " + description.toString())
        println("Label: " + label)
        println("Brief: " + brief)
        println("Type: " + datatype)
    }
}
