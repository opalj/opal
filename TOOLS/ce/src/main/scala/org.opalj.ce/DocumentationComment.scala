/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

import org.apache.commons.text.StringEscapeUtils

/**
 * Container for the comments of a config node.
 */
class DocumentationComment(val label: String, val brief: String, val description: Seq[String], val datatype: String, val constraints: Seq[String]) {

    /**
     * Converts the Comment object into HTML syntax.
     * @return returns the entire object as HTML code ready for insertion into an HTML file.
     */
    def toHTML: String = {
        val HTMLString = new StringBuilder()
        if (!isEmpty) {
            if (description.mkString("").trim.nonEmpty) {
                HTMLString ++= "<p><b> Description: </b> <br>\n"
                HTMLString ++= s"${StringEscapeUtils.escapeHtml4(description.mkString("\n")).replace("\n", "<br>\n")} <br> </p>\n"
            }
            if (datatype.nonEmpty)
                HTMLString ++= s"<p><b> Type: </b>${StringEscapeUtils.escapeHtml4(datatype)}<br></p>\n"
            if (constraints.nonEmpty) {
                if (datatype.equals("enum")) {
                    HTMLString ++= "<p><b> Allowed Values: </b><br>\n"
                } else {
                    HTMLString ++= "<p><b> Constraints: </b><br>\n"
                }
                HTMLString ++= s"${StringEscapeUtils.escapeHtml4(constraints.mkString("\n")).replace("\n","<br>\n")} <br>\n </p>\n"
            }
        }
        HTMLString.toString
    }

    /**
     * Merges another comment into this comment.
     * The datatype flag will not be merged. Reason for this is that datatypes are only used to describe entries, which cannot be merged anyways.
     *
     * @param comment accepts the comment that should be merged into this comment.
     */
    def mergeComment(comment: DocumentationComment): DocumentationComment = {
        val mergedLabel = if (label != "" && comment.label != "") {
            s"${comment.label}.$label"
        } else {
            s"${comment.label}$label"
        }
        val mergedBrief = s"${comment.brief} $brief".trim
        val mergedDescription = description ++ comment.description
        val mergedConstraints = constraints ++ comment.constraints

        new DocumentationComment(mergedLabel, mergedBrief, mergedDescription, "", mergedConstraints)
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
    def replaceClasses(se: SubclassExtractor): DocumentationComment = {
        if (datatype.equals("subclass")) {
            // Get a Set of all subclasses
            val root = constraints.head

            // Replace Types
            val updatedConstraints = constraints ++ se.extractSubclasses(root)

            new DocumentationComment(label, brief, description, "enum", updatedConstraints)
        } else {
            this
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
object DocumentationComment {
    /**
     *  Factory method for creating a comment.
     *  @param commentBuffer accepts a ListBuffer that contains the raw content of the comment.
     *  @return is a fully functional Comment.
     */
    def fromString(commentBuffer: ListBuffer[String]): DocumentationComment = {
        var label = ""
        var brief = ""
        val description = ListBuffer[String]()
        var datatype = ""
        val constraints = ListBuffer[String]()
        for (line <- commentBuffer) {
            val trimmedLine = line.trim
            if (trimmedLine.startsWith("@label")) {
                label = trimmedLine.stripPrefix("@label").trim
            } else if (trimmedLine.startsWith("@brief")) {
                brief = trimmedLine.stripPrefix("@brief").trim
            } else if (trimmedLine.startsWith("@constraint")) {
                constraints += trimmedLine.stripPrefix("@constraint").trim
            } else if (trimmedLine.startsWith("@type")) {
                datatype = trimmedLine.stripPrefix("@type").trim
            } else {
                description += trimmedLine.stripPrefix("@description").trim
            }
        }
        new DocumentationComment(label, brief, description.toSeq, datatype, constraints.toSeq)
    }
}
