package org.opalj.ce

import java.nio.file.Path
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.control.Breaks.break

class CommentParser() {


    def parseComments(filePath: Path): ConfigNode = {
        val lines = Source.fromFile(filePath.toString()).getLines().toList
        val iterator = lines.iterator
        val (node,remains) = parseObject(iterator, "", new Comment)
        node
    }

    def parseObject(iterator: Iterator[String], lastLine: String, currentComment : Comment) : (ConfigObject,String) = {
        val entries = mutable.Map[String, ConfigNode]()
        var line : String = lastLine
        val nextComment = new Comment
        var currentKey = ""
        var currentvalue : ConfigNode = null

        while(iterator.hasNext){
            if(line.trim.startsWith("#") || line.trim.startsWith("//")){
                nextComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
                line = ""
            } else if (line.trim.startsWith("}")) {
                // Found the closing bracket of the object. Remove the closing bracket and stop parsing the object
                line = line.trim.stripPrefix("}")
                break()
            } else {
                // If it is none of these apply, the following string is a key. We need to identify, what terminates the key. This can be a ':', a '=', a '[' or a '{'
                // However, it is allowed to run multiple objects within these, so we need to find out what comes FIRST
                val terminatingChars = Set(':','=','{','[')
                val terminatingIndex = this.findIndexOfCharsetInString(terminatingChars,line)

                currentKey = line.substring(0,terminatingIndex -1)
                line = line.substring(terminatingIndex).trim.stripPrefix(":").stripPrefix("=")

                if(line.trim.startsWith("{")){
                    // value begins with opening bracket of an object
                    val (newvalue,newline) = this.parseObject(iterator, line, nextComment)
                    line = newline
                    currentvalue = newvalue

                } else if(line.trim.startsWith("[")){
                    // value begins with opening bracket of an object
                    val (newvalue,newline) = this.parseList(iterator, line, nextComment)
                    line = newline
                    currentvalue = newvalue
                } else {
                    // value is an entry
                    val (newvalue,newline) = this.parseEntry(iterator, line, nextComment)
                    line = newline
                    currentvalue = newvalue
                }
                line = line.trim.stripPrefix(",")
                entries.addOne((currentKey,currentvalue))
            }

            if(line.trim == ""){
                line = iterator.next()
            }
        }

        // If there is a comment directly behind the closing bracket of the object, add it to comments too.
        if(line.trim.startsWith("#") || line.trim.startsWith("//")){
            currentComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
            line = ""
        }

        currentComment.commitComments()
        (ConfigObject(entries.toMap, currentComment),"")
    }

    def parseEntry(iterator: Iterator[String], lastLine : String, currentComment : Comment) : (ConfigEntry,String) = {
        var line: String = lastLine
        var value = ""

        if (line.trim.startsWith("#") || line.trim.startsWith("//")) {
            // Case: line starts with a comment
            while(line.trim.startsWith("#") || line.trim.startsWith("//")) {
                currentComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
                line = iterator.next()
            }
        } else if (line.trim.startsWith("\"\"\"")){
            // Case: line starts with a triple quoted string (These allow for multi-line values, so the line end does not necessarily terminate the value
            val openedvalue  = line.trim.stripPrefix("\"\"\"")
            val (newline,newvalue) = this.extractValue(iterator,openedvalue,"\"\"\"")
            line = newline
            value = newvalue
        } else if (line.trim.startsWith("\"")) {
            // Case: line starts with a quoted string
            line = line.trim.stripPrefix("\"").trim
            value = line.substring(0,line.indexOf("\"")-1).trim
            line = line.stripPrefix(value).trim.stripPrefix("\"")
        } else if (line.trim.startsWith("\'")) {
            // Case: line starts with a single quoted string
            line = line.trim.stripPrefix("\'").trim
            value = line.substring(0,line.indexOf("\'")-1).trim
            line = line.stripPrefix(value).trim.stripPrefix("\'")
        } else {
            // Case: Line starts with an unquoted string
            // There are two ways of terminating an unquoted string
            // Option 1: The value is inside of a pattern that has other control structures
            val terminatingChars = Set(',',']','}',' ')
            val terminatingIndex = this.findIndexOfCharsetInString(terminatingChars,line)

            value = line.trim.substring(0,terminatingIndex-1).trim
            line = line.trim.stripPrefix(value).trim
        }

        // If a comment is behind the value in the same line, this adds it to the comments too
        if(line.trim.startsWith("#") || line.trim.startsWith("//")){
            currentComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
            line = ""
        }

        currentComment.commitComments()
        (ConfigEntry(value,currentComment),line)
    }

    def parseList(iterator: Iterator[String], lastLine: String, currentComment : Comment) : (ConfigList,String) = {
        var line = lastLine
        val value = new ListBuffer[ConfigNode]
        var nextComment = new Comment

        while(iterator.hasNext){
            if(line.trim.startsWith("{")){
                val (configobject,newline) = parseObject(iterator, line.trim.stripPrefix("{"), nextComment)
                value += configobject
                line = newline
                nextComment = new Comment
            } else if (line.trim.startsWith("[")){
                val (configlist,newline) = parseList(iterator, line.trim.stripPrefix("["), nextComment)
                value += configlist
                line = newline
                nextComment = new Comment
            } else if(line.trim.startsWith("//") || line.trim.startsWith("#")){
                nextComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
                line = ""
            } else if(line.trim.startsWith("]") || (line.trim.startsWith(",") && line.trim.stripPrefix(",").trim.startsWith("]"))){
                line = line.trim.stripPrefix(",").trim.stripPrefix("]")
                break()
            } else {
                val (configEntry,newline) = parseEntry(iterator, line.trim.stripPrefix(","), nextComment)
                value += configEntry
                line = newline
            }

            if(line.trim == ""){
                line = iterator.next()
            }
        }
        if(line.trim.startsWith("#") || line.trim.startsWith("//")){
            currentComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
            line = ""
        }

        currentComment.commitComments()
        (ConfigList(value, currentComment),line)
    }

    private def extractValue(iterator: Iterator[String], line : String, terminatingSymbol : String): (String,String) = {
        var value = ""
        var remainingLine = ""
        if(line.contains(terminatingSymbol)) {
            value = line.substring(0,line.indexOf(terminatingSymbol))
            remainingLine = line.substring(line.indexOf(terminatingSymbol)).stripPrefix(terminatingSymbol)
        } else {
            value = line
            while(iterator.hasNext){
                remainingLine = iterator.next()
                if(remainingLine.contains(terminatingSymbol)){
                    value += remainingLine.trim.substring(0,remainingLine.trim.indexOf(terminatingSymbol))
                    remainingLine = remainingLine.trim.stripPrefix(remainingLine.trim.substring(0,remainingLine.trim.indexOf(terminatingSymbol))).stripPrefix(terminatingSymbol)
                    break()
                } else {
                    value += remainingLine.trim()
                }
            }
        }
        (value,remainingLine)
    }

    private def findIndexOfCharsetInString(characterSet : Set[Char], string: String): Int = {
        string.zipWithIndex.collectFirst {
            case(char,index) if characterSet.contains(char) => index
        }.getOrElse(-1)
    }
}