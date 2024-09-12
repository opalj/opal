package org.opalj.ce

import java.nio.file.Path
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.control.Breaks._

/**
 * The Comment Parser is responsible for parsing the HOCON files and its comments
 * Do NOT use this class directly, use ConfigParserWrapper for safe parsing instead
 */
class CommentParser() {

    /**
     * parseComments initiates the parsing process
     * A ConfigNode can consist out of 3 possible types that are parsed differently: Objects, Lists and Entries
     * The source node of a config file always is an object
     * During parsing, the Parser will iterate the file and sort it into the ConfigNode structure.
     * Since the Nodes can be nested and there can be multiple Nodes in one line, the Parser needs to examine most control structures like a stream and not linewise
     * @param filePath accepts the path to a valid HOCON file
     * @return returns the fully parsed file as a configObject
     */
    def parseComments(filePath: Path): ConfigObject = {
        val lines = Source.fromFile(filePath.toString()).getLines().toList
        val iterator = lines.iterator
        val initialComment = new Comment
        initialComment.addComment("@label " + filePath.toString.substring(filePath.toString.indexOf("opal") + 4))
        val (node,remains) = parseObject(iterator, "", initialComment)
        node
    }

    /**
     * Method responsible to parse Object-Type Nodes
     * @param iterator accepts the iterator over the current config file
     * @param lastLine accepts the remains of the line that belongs to this object
     * @param currentComment assings previously parsed comment to this Node. This is necessary as most comments appear before the opening bracket of an object (Which identifies it as an object)
     * @return returns the fully parsed object
     */
    private def parseObject(iterator: Iterator[String], lastLine: String, currentComment : Comment) : (ConfigObject,String) = {
        // Creating necessary components
        val entries = mutable.Map[String, ConfigNode]()
        var line : String = lastLine
        var nextComment = new Comment
        var currentKey = ""
        var currentvalue : ConfigNode = null

        // Using a breakable while loop to interrupt as soon as the object ends
        breakable { while(iterator.hasNext){

            if(line.trim.startsWith("#") || line.trim.startsWith("//")){
                // Found a comment. Comments are terminated by the end of the line. Add the entire line to the comments and continue with the next one
                nextComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
                line = ""

            } else if (line.trim.startsWith("}")) {
                // Found the closing bracket of the object. Remove the closing bracket and stop parsing the object
                line = line.trim.stripPrefix("}")
                break()

            } else if(line.trim != ""){
                // If none of the options above apply and the line is NOT empty (in which case load the next line and ignore this)
                // What follows now is part of the content of the object
                // Objects are Key Value pairs, so parsing these is a two stage job: Separating Key and value and then parsing the value

                // 1. Separating Key and value
                // In JSON, Keys and values are separated with ':'. HOCON allows substituting ':' with '=' and also allows ommitting these symbols when using a '{' or '[' to open an object/list afterwards
                // Finding first instance of these symbols
                // TerminatingIndex is the index of the symbol that terminates the key.
                val terminatingChars = Set(':','=','{','[')
                val terminatingIndex = this.findIndexOfCharsetInString(terminatingChars,line)

                // Splitting the key from the string (while splitting of the ':' or '=' as they are not needed anymore
                currentKey = line.substring(0,terminatingIndex -1)
                line = line.substring(terminatingIndex).trim.stripPrefix(":").stripPrefix("=")

                // Evaluating the type of value
                if(line.trim.startsWith("{")){
                    // Case: Value is an object
                    val (newvalue,newline) = this.parseObject(iterator, line.trim.substring(1), nextComment)
                    line = newline
                    currentvalue = newvalue

                } else if(line.trim.startsWith("[")){
                    // Case: Value is a list
                    val (newvalue,newline) = this.parseList(iterator, line.trim.substring(1), nextComment)
                    line = newline
                    currentvalue = newvalue
                } else {
                    // Case: Value is an entry
                    val (newvalue,newline) = this.parseEntry(iterator, line, nextComment)
                    line = newline
                    currentvalue = newvalue
                }

                // Reset next comment
                nextComment = new Comment

                // Json Keys are split using a ",". This is not necessary, but tolerated in HOCON syntax
                line = line.trim.stripPrefix(",")

                // Adding the new Key, Value pair to the Map
                entries.addOne((currentKey,currentvalue))
            }

            // Proceed with the next line if the current one was fully parsed
            if(line.trim == "" && iterator.hasNext){
                line = iterator.next()
            }
        }}

        // If there is a comment directly behind the closing bracket of the object, add it to comments too.
        if(line.trim.startsWith("#") || line.trim.startsWith("//")){
            currentComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
            line = ""
        }

        // Start the internal parser of the comment
        currentComment.commitComments()

        // Return the finished ConfigObject
        (ConfigObject(entries.toMap, currentComment),"")
    }

    /**
     * Method responsible to parse Entry-Type Nodes
     * @param iterator accepts the iterator over the current config file
     * @param lastLine accepts the remains of the line that belongs to this object
     * @param currentComment assings previously parsed comment to this Node. This is necessary as most comments appear before the opening bracket of an object (Which identifies it as an object)
     * @return returns the fully parsed entry
     */
    private def parseEntry(iterator: Iterator[String], lastLine : String, currentComment : Comment) : (ConfigEntry,String) = {
        // Creation of necessary values
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
            // Case: line starts with a double quoted string
            line = line.trim.stripPrefix("\"").trim
            // A '\' can escape a quote. Thus we need to exclude that from the terminating Index
            val terminatingIndices = line.zipWithIndex.collect {
                case (char, index) if char == '\"' => index
            }
            var terminatingIndex = line.length

            for(i <- 0 to terminatingIndices.length -1){
                if((terminatingIndices(i) == 0 || line((terminatingIndices(i)-1)) != '\\') && terminatingIndex == line.length){
                    terminatingIndex = terminatingIndices(i)
                }
            }

            value = line.substring(0,terminatingIndex).trim
            line = line.stripPrefix(value).trim.stripPrefix("\"")
        } else if (line.trim.startsWith("\'")) {
            // Case: line starts with a single quoted string
            line = line.trim.stripPrefix("\'").trim
            value = line.substring(0,line.indexOf("\'")).trim
            line = line.stripPrefix(value).trim.stripPrefix("\'")
        } else {
            // Case: Line starts with an unquoted string
            // There are two ways of terminating an unquoted string
            // Option 1: The value is inside of a pattern that has other control structures
            line = line.trim
            val terminatingChars = Set(',',']','}',' ')
            val terminatingIndex = this.findIndexOfCharsetInString(terminatingChars,line)

            if(terminatingIndex > 0) {
                value = line.trim.substring(0, terminatingIndex).trim
                line = line.trim.stripPrefix(value).trim
            } else {
                // Option 2: The end of the line
                value = line.trim
                line = ""
            }
        }

        // If a comment is behind the value in the same line, this adds it to the comments too
        if(line.trim.startsWith("#") || line.trim.startsWith("//")){
            currentComment.addComment(line.trim.stripPrefix("#").stripPrefix("//").trim)
            line = ""
        }

        currentComment.commitComments()
        (ConfigEntry(value,currentComment),line)
    }

    private def parseList(iterator: Iterator[String], lastLine: String, currentComment : Comment) : (ConfigList,String) = {
        var line = lastLine
        val value = new ListBuffer[ConfigNode]
        var nextComment = new Comment

        breakable { while(iterator.hasNext){
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

            if(line.trim == "" && iterator.hasNext){
                line = iterator.next()
            }
        }}
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
            breakable { while(iterator.hasNext){
                remainingLine = iterator.next()
                if(remainingLine.contains(terminatingSymbol)){
                    value += remainingLine.trim.substring(0,remainingLine.trim.indexOf(terminatingSymbol))
                    remainingLine = remainingLine.trim.stripPrefix(remainingLine.trim.substring(0,remainingLine.trim.indexOf(terminatingSymbol))).stripPrefix(terminatingSymbol)
                    break()
                } else {
                    value += remainingLine.trim()
                }
            }}
        }
        (value,remainingLine)
    }

    private def findIndexOfCharsetInString(characterSet : Set[Char], string: String): Int = {
        string.zipWithIndex.collectFirst {
            case(char,index) if characterSet.contains(char) => index
        }.getOrElse(-1)
    }
}