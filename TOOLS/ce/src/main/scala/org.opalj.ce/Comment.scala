package org.opalj.ce

import scala.collection.mutable.ListBuffer

class Comment {
    val commentBuffer: ListBuffer[String] = ListBuffer[String]()

    // Adds a comment to the comment buffer. If a comment has multiple lines, this method will be called multiple times
    def addComment(comment: String): Unit = {
        this.commentBuffer += comment
    }

    // Tells the Comment object that no more comments will be added. The comment object then begins parsing the comments into advanced objects
    def commitComments(): Unit = {

    }

    override def toString() : String = {
        commentBuffer.toString()
    }
}
