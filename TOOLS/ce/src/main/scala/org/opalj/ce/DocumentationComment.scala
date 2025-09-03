/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import scala.collection.mutable.ListBuffer

import org.apache.commons.text.StringEscapeUtils

/**
 * Container for the comments of a config node.
 */
class DocumentationComment(
    val label:       String,
    val brief:       String,
    val description: Seq[String],
    val datatype:    String,
    val constraints: Seq[String]
) {

    /**
     * Converts the Comment object into HTML syntax.
     * @param pageHTML The method will add the export to this StringBuilder.
     */
    def toHTML(pageHTML: StringBuilder): Unit = {
        if (!isEmpty) {
            if (description.mkString("").trim.nonEmpty) {
                pageHTML ++= "<p><b> Description: </b> <br>\n"
                pageHTML ++= StringEscapeUtils.escapeHtml4(description.mkString("\n")).replace("\n", "<br>\n")
                pageHTML ++= "<br> </p>\n"
            }
            if (datatype.nonEmpty) {
                pageHTML ++= s"<p><b> Type: </b>"
                pageHTML ++= StringEscapeUtils.escapeHtml4(datatype)
                pageHTML ++= "<br></p>\n"
            }
            if (constraints.nonEmpty) {
                if (datatype.equals("enum")) {
                    pageHTML ++= "<p><b> Allowed Values: </b><br>\n"
                } else {
                    pageHTML ++= "<p><b> Constraints: </b><br>\n"
                }
                pageHTML ++= StringEscapeUtils.escapeHtml4(constraints.mkString("\n")).replace("\n", "<br>\n")
                pageHTML ++= "<br>\n </p>\n"
            }
        }
    }

    /**
     * Merges another comment into this comment.
     * The datatype flag will not be merged. Reason for this is that datatypes are only used to describe entries, which cannot be merged anyways.
     * @param comment accepts the comment that should be merged into this comment.
     * @return returns a merged DocumentationComment.
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
        description.isEmpty && constraints.isEmpty && datatype.isEmpty
    }

    /**
     * Method used for fetching information of the brief field.
     * @param previewDescriptionLength accepts an integer that determines the maximum amount of characters that the fallback brief preview can contain.
     * @return Returns the brief field of the DocumentationComment if it exists. If it does not exist, it returns a preview of the description.
     */
    def getBrief(previewDescriptionLength: Int): String = {
        if (brief.isEmpty && description.nonEmpty)
            description.head.substring(0, description.head.length.min(previewDescriptionLength)) +
                (if (description.size > 1 || description.head.length > previewDescriptionLength) "..." else "")
        else brief
    }

    /**
     * This method is responsible for finding all subclasses to a subclass type and adds them to the constraints.
     * Then, it changes its datatype to enum to show that all allowed values are the listed classes.
     * @param se Accepts an initialized subclass extractor. It accesses the ClassHierarchy that was extracted by the subclass extractor and finds its subclasses within the structure.
     * @return Returns an Updated DocumentationComment if there were classes to replace. Returns itself otherwise.
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
}

/**
 * Factory method for creating a Comment.
 */
object DocumentationComment {
    /**
     *  Factory method for creating a comment.
     *  @param commentLines the raw content of the comment in the form of trimmed lines
     *  @return the Comment
     */
    def fromString(commentLines: scala.collection.Seq[String]): DocumentationComment = {
        var label = ""
        var brief = ""
        val description = ListBuffer[String]()
        var datatype = ""
        val constraints = ListBuffer[String]()
        for (line <- commentLines) {
            if (line.startsWith("@label")) {
                label = line.stripPrefix("@label").trim
            } else if (line.startsWith("@brief")) {
                brief = line.stripPrefix("@brief").trim
            } else if (line.startsWith("@constraint")) {
                constraints += line.stripPrefix("@constraint").trim
            } else if (line.startsWith("@type")) {
                datatype = line.stripPrefix("@type").trim
            } else {
                description += line.stripPrefix("@description").trim
            }
        }
        new DocumentationComment(label, brief, description.toSeq, datatype, constraints.toSeq)
    }
}
