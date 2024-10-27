/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

/**
 * Container for the comments of a config node.
 */
class Comment {
    val constraints: ListBuffer[String] = ListBuffer[String]()
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
        if (!isEmpty) {
            if (description.nonEmpty) {
                HTMLString += "<p><b> Description: </b> <br>\n"
                for (line <- description) {
                    HTMLString += line + "<br>\n"
                }
                HTMLString += "</p>\n"
            }
            if (datatype.nonEmpty) HTMLString += "<p><b> Type: </b>" + datatype + "<br></p>\n"
            if (constraints.nonEmpty) {
                if (datatype.equals("enum")) {
                    HTMLString += "<p><b> Allowed Values: </b><br>\n"
                } else {
                    HTMLString += "<p><b> Constraints: </b><br>\n"
                }
                for (line <- constraints) {
                    HTMLString += line + "<br>\n"
                }
                HTMLString += "</p>\n"
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
        if (label != "" && comment.label != "") {
            label = comment.label + "." + label
        } else {
            label = comment.label + label
        }
        brief = comment.brief + "   " + brief
        brief = brief.trim
        description.addAll(comment.description)
        constraints.addAll(comment.constraints)
    }

    /**
     * Checks if the comment is empty.
     * @return returns true if the comment is empty but the label property (the label property is set automatically for config files.).
     */
    def isEmpty: Boolean = {
        if (description.isEmpty && constraints.isEmpty && datatype.isEmpty) return true
        false
    }

    /**
     * Method used for fetching information of the brief field.
     * @return Returns the brief field of the DocumentationComment if it exists. If it does not exist, it returns a preview of the description.
     */
    def getBrief(previewDescriptionLength: Int): String = {
        if (brief.isEmpty) {
            if (description.nonEmpty) {
                return description.head.substring(0, description.head.length.min(previewDescriptionLength)) + "..."
            }
        }
        brief
    }

    /**
     * This method is responsible for finding all subclasses to a subclass type and adds them to the constraints.
     * Then, it changes its datatype to enum to show that all allowed values are the listed classes.
     * @param se Accepts an initialized subclass extractor. It accesses the ClassHierarchy that was extracted by the subclass extractor and finds its subclasses within the structure.
     */
    def replaceClasses(se: SubclassExtractor): Unit = {
        if (datatype.equals("subclass")) {
            // Get a Set of all subclasses
            val root = constraints.head

            // Replace Types
            datatype = "enum"
            constraints ++= ListBuffer(se.extractSubclasses(root).toSeq: _*)
        }
    }
    /**
     * Prints the Comment object to the console.
     * Debug purposes.
     */
    def printObject(): Unit = {
        println("Constraints: " + constraints.toString())
        println("Description: " + description.toString())
        println("Label: " + label)
        println("Brief: " + brief)
        println("Type: " + datatype)
    }
}

/**
 * Factory method for creating a Comment.
 */
object Comment {
    /**
     *  Factory method for creating a comment.
     *  @param commentBuffer accepts a ListBuffer that contains the raw content of the comment.
     *  @return is a fully functional Comment.
     */
    def fromString(commentBuffer: ListBuffer[String]): Comment = {
        val comment = new Comment()
        for (line <- commentBuffer) {
            val trimmedLine = line.trim
            if (trimmedLine.startsWith("@label")) {
                comment.label = trimmedLine.stripPrefix("@label").trim
            } else if (trimmedLine.startsWith("@brief")) {
                comment.brief = trimmedLine.stripPrefix("@brief").trim
            } else if (trimmedLine.startsWith("@constraint")) {
                comment.constraints += trimmedLine.stripPrefix("@constraint").trim
            } else if (trimmedLine.startsWith("@type")) {
                comment.datatype = trimmedLine.stripPrefix("@type").trim
            } else {
                comment.description += trimmedLine.stripPrefix("@description").trim
            }
        }
        comment
    }
}
