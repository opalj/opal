/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.nio.file.Path
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Using
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * The Comment Parser is responsible for parsing the HOCON files and its comments.
 * Do NOT use this class directly, use ConfigParserWrapper for safe parsing instead.
 */
class CommentParser() {
    private var iterator: Iterator[String] = Iterator.empty
    private var line = ""
    implicit val logContext: LogContext = GlobalLogContext

    /**
     * parseComments initiates the parsing process.
     * A ConfigNode can consist out of 3 possible types that are parsed differently: Objects, Lists and Entries.
     * The source node of a config file always is an object.
     * During parsing, the Parser will iterate the file and sort it into the ConfigNode structure.
     * Since the Nodes can be nested and there can be multiple Nodes in one line, the Parser needs to examine most control structures like a stream and not linewise.
     * @param filePath accepts the path to a valid HOCON file.
     * @return returns the fully parsed file as a configObject.
     */
    def parseFile(filePath: Path, rootDirectory: Path): ConfigObject = {
        // Initialize iterator
        OPALLogger.info("Configuration Explorer", s"Parsing: ${filePath.toString}")

        Using.resource(Source.fromFile(filePath.toString)) { source =>
            iterator = source.getLines()

            // Parse initial Comments
            val initialComment = ListBuffer[String]()
            initialComment += ("@label " + filePath.toAbsolutePath.toString.stripPrefix(
                rootDirectory.toAbsolutePath.toString
            ))
            initialComment ++= parseComments()
            parseObject(initialComment)
        }
    }

    /**
     * Method responsible to parse Object-Type Nodes.
     * @param currentComment assigns previously parsed comment to this Node. This is necessary as most comments appear before the opening bracket of an object (Which identifies it as an object).
     * @return returns the fully parsed object.
     */
    private def parseObject(currentComment: ListBuffer[String]): ConfigObject = {
        line = line.trim.stripPrefix("{")
        // Creating necessary components
        val entries = mutable.Map[String, ConfigNode]()
        var nextComment = parseComments()
        var currentKey = ""
        var currentvalue: ConfigNode = null

        // Using a breakable while loop to interrupt as soon as the object ends
        breakable {
            while (iterator.hasNext || line.nonEmpty) {
                parseComments(nextComment)
                if (line.trim.startsWith("}")) {
                    // Found the closing bracket of the object. Remove the closing bracket and stop parsing the object
                    line = line.trim.stripPrefix("}")
                    break()

                } else if (line.trim != "") {
                    // If none of the options above apply and the line is NOT empty (in which case load the next line and ignore this)
                    // What follows now is part of the content of the object
                    // Objects are Key Value pairs, so parsing these is a two stage job: Separating Key and value and then parsing the value

                    // 1. Separating Key and value
                    // In JSON, Keys and values are separated with ':'. HOCON allows substituting ':' with '=' and also allows ommitting these symbols when using a '{' or '[' to open an object/list afterwards
                    // Finding first instance of these symbols
                    // TerminatingIndex is the index of the symbol that terminates the key.
                    val terminatingChars = Set(':', '=', '{', '[')
                    val terminatingIndex = findIndexOfCharsetInString(terminatingChars, line)

                    // Splitting the key from the string (while splitting of the ':' or '=' as they are not needed anymore
                    currentKey = line.substring(0, terminatingIndex - 1).trim.stripPrefix("\"").stripSuffix("\"")
                    line = line.substring(terminatingIndex).trim.stripPrefix(":").stripPrefix("=")

                    // Evaluating the type of value
                    if (line.trim.startsWith("{")) {
                        // Case: Value is an object
                        currentvalue = parseObject(nextComment)
                    } else if (line.trim.startsWith("[")) {
                        // Case: Value is a list
                        currentvalue = parseList(nextComment)
                    } else {
                        // Case: Value is an entry
                        currentvalue = parseEntry(nextComment)
                    }

                    // Reset next comment
                    nextComment = ListBuffer[String]()

                    // Json Keys are split using a ",". This is not necessary, but tolerated in HOCON syntax
                    line = line.trim.stripPrefix(",")

                    // Adding the new Key, Value pair to the Map
                    entries += ((currentKey, currentvalue))
                }

                // Proceed with the next line if the current one was fully parsed
                if (line.trim == "" && iterator.hasNext) {
                    line = iterator.next()
                }
            }
        }

        // If there is a comment directly behind the closing bracket of the object, add it to comments too.
        if (line.trim.startsWith("#") || line.trim.startsWith("//")) {
            currentComment += line.trim.stripPrefix("#").stripPrefix("//").trim
            line = ""
        }

        // Return the finished ConfigObject
        ConfigObject(entries, DocumentationComment.fromString(currentComment))
    }

    /**
     * Method responsible to parse Entry-Type Nodes.
     * @param currentComment assings previously parsed comment to this Node. This is necessary as most comments appear before the opening bracket of an object (Which identifies it as an object).
     * @return returns the fully parsed entry.
     */
    private def parseEntry(currentComment: ListBuffer[String]): ConfigEntry = {
        // Creation of necessary values
        var value = ""

        parseComments(currentComment)

        if (line.trim.startsWith("\"\"\"")) {
            // Case: line starts with a triple quoted string (These allow for multi-line values, so the line end does not necessarily terminate the value
            line = line.trim.stripPrefix("\"\"\"")
            value = extractValue("\"\"\"")
        } else if (line.trim.startsWith("\"")) {
            // Case: line starts with a double quoted string
            line = line.trim.stripPrefix("\"").trim
            // A '\' can escape a quote. Thus we need to exclude that from the terminating Index
            var terminatingIndex = line.length
            var i = 0
            breakable(while (i < line.length) {
                if (line(i) == '\"' && (i == 0 || line(i - 1) != '\\')) {
                    terminatingIndex = i
                    // Sobald wir das erste gültige Anführungszeichen finden, beenden wir die Schleife
                    break()
                }
                i += 1
            })

            value = line.substring(0, terminatingIndex).trim
            line = line.stripPrefix(value).trim.stripPrefix("\"")
        } else if (line.trim.startsWith("\'")) {
            // Case: line starts with a single quoted string
            line = line.trim.stripPrefix("\'").trim
            value = line.substring(0, line.indexOf("\'")).trim
            line = line.stripPrefix(value).trim.stripPrefix("\'")
        } else {
            // Case: Line starts with an unquoted string
            // There are two ways of terminating an unquoted string
            // Option 1: The value is inside of a pattern that has other control structures
            line = line.trim
            val terminatingChars = Set(',', ']', '}', ' ')
            val terminatingIndex = findIndexOfCharsetInString(terminatingChars, line)

            if (terminatingIndex > 0) {
                value = line.trim.substring(0, terminatingIndex).trim
                line = line.trim.stripPrefix(value).trim
            } else {
                // Option 2: The end of the line
                value = line.trim
                line = ""
            }
        }

        // If a comment is behind the value in the same line, this adds it to the comments too
        if (line.trim.startsWith("#") || line.trim.startsWith("//")) {
            currentComment += line.trim.stripPrefix("#").stripPrefix("//").trim
            line = ""
        }

        ConfigEntry(value, DocumentationComment.fromString(currentComment))
    }

    /**
     * Method responsible to parse List-Type Nodes.
     * @param currentComment assings previously parsed comment to this Node. This is necessary as most comments appear before the opening bracket of an object (Which identifies it as an object).
     * @return returns the fully parsed entry.
     */
    private def parseList(currentComment: ListBuffer[String]): ConfigList = {
        line = line.trim.stripPrefix("[")
        // Creating necessary variables
        val value = new ListBuffer[ConfigNode]
        var nextComment = ListBuffer[String]()

        breakable {
            while (iterator.hasNext || line.nonEmpty) {
                nextComment = parseComments()
                if (line.trim.startsWith("{")) {
                    // Case: The following symbol opens an object
                    value += parseObject(nextComment)
                    line = line.trim.stripPrefix(",")
                } else if (line.trim.startsWith("[")) {
                    // Case: The following symbol opens a list
                    value += parseList(nextComment)
                    line = line.trim.stripPrefix(",")
                } else if (line.trim.startsWith("]") || (line.trim.startsWith(",") && line.trim.stripPrefix(
                               ","
                           ).trim.startsWith("]"))
                ) {
                    // Case: The following symbol closes the list
                    line = line.trim.stripPrefix(",").trim.stripPrefix("]")
                    break()
                } else if (line.trim != "") {
                    // Case: The following symbol is an entry
                    value += parseEntry(nextComment)
                    line = line.trim.stripPrefix(",")
                }

                if (line.trim == "" && iterator.hasNext) {
                    // Load next line when done
                    line = iterator.next()
                }
            }
        }
        if (line.trim.startsWith("#") || line.trim.startsWith("//")) {
            // Add the comment in the same line of the list as well
            currentComment += line.trim.stripPrefix("#").stripPrefix("//").trim
            line = ""
        }

        // Finish
        ConfigList(value, DocumentationComment.fromString(currentComment))
    }

    /**
     * Internal method for finding the end of a multi line value.
     * @param terminatingSymbol is the string that terminates the value.
     * @return returns a tuple of the parsed value and the substring of the line that has not been parsed yet.
     */
    private def extractValue(terminatingSymbol: String): String = {
        // creating necessary variables
        var value = ""
        var remainingLine = ""

        if (line.contains(terminatingSymbol)) {
            // The value is a single line value
            value = line.substring(0, line.indexOf(terminatingSymbol))
            remainingLine = line.substring(line.indexOf(terminatingSymbol)).stripPrefix(terminatingSymbol)
        } else {
            // The value is a multi line value
            value = line
            breakable {
                while (iterator.hasNext) {
                    remainingLine = iterator.next()
                    if (remainingLine.contains(terminatingSymbol)) {
                        value += remainingLine.trim.substring(0, remainingLine.trim.indexOf(terminatingSymbol))
                        remainingLine = remainingLine.trim.stripPrefix(remainingLine.trim.substring(
                            0,
                            remainingLine.trim.indexOf(terminatingSymbol)
                        )).stripPrefix(terminatingSymbol)
                        break()
                    } else {
                        value += remainingLine.trim()
                    }
                }
            }
        }
        line = remainingLine
        value
    }

    /**
     * Method that finds the first instance of a character out of a character set.
     * @param characterSet accepts the selection of characters that is supposed to be looked for.
     * @param string accepts the string that is to be searched.
     * @return returns the lowest index of any character out of the character set that occurs in the string.
     * @return returns -1 if none of the characters appear in the string.
     */
    private def findIndexOfCharsetInString(characterSet: Set[Char], string: String): Int = {
        string.zipWithIndex.collectFirst {
            case (char, index) if characterSet.contains(char) => index
        }.getOrElse(-1)
    }

    /**
     * Gets all comments until the next non-comment entry and writes them into a new ListBuffer.
     * @return Returns a new ListBuffer with the content of the comment.
     */
    private def parseComments(): ListBuffer[String] = {
        val comment = new ListBuffer[String]
        parseComments(comment)
    }

    /**
     * Gets all comments all comments until the next non-comment entry and writes them into an existing ListBuffer.
     * @param comment Accepts the ListBuffer that the following comment should be added to.
     * @return Returns the existing ListBuffer, but with the content of the comment added to it.
     */
    private def parseComments(comment: ListBuffer[String]): ListBuffer[String] = {
        while (line.trim.startsWith("#") || line.trim.startsWith("//") || line.trim == "") {
            if (line.trim != "") comment += line.trim.stripPrefix("#").stripPrefix("//")
            line = iterator.next()
        }
        comment
    }
}
