/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ce

import java.nio.file.Path
import scala.collection.immutable
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Using
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable

import com.typesafe.config.ConfigFactory

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * The class CommentParserWrapper is the class that should be used for parsing commented config within the Configuration Explorer.
 */
class CommentParserWrapper {
    /**
     * Made to parse multiple Configuration Files in bulk.
     * Used in combination with the file Locator to locate and parse all config files of a project.
     * @param filepaths accepts a list of full paths to the HOCON config files that shall be parsed.
     * @return is a list of the parsed configuration files, paired with the path they originate from.
     */
    def iterateConfigs(filepaths: Iterable[Path], rootDirectory: Path): immutable.Seq[ConfigObject] = {
        val commentedConfigs = new ListBuffer[ConfigObject]
        for (filepath <- filepaths) {
            commentedConfigs += ParseComments(filepath, rootDirectory)
        }

        // Merge all config files named "reference.conf"
        val mergingConfigs = new ListBuffer[ConfigObject]
        for (i <- commentedConfigs.indices.reverse) {
            val config = commentedConfigs(i)
            if (config.comment.label.endsWith("reference.conf")) {
                mergingConfigs += config
                commentedConfigs.remove(i)
            }
        }
        if (mergingConfigs.nonEmpty) {
            val conf = ConfigObject(mutable.Map[String, ConfigNode](), new DocumentationComment)
            for (i <- mergingConfigs.indices) {
                conf.merge(mergingConfigs(i))
            }
            conf.comment.label = "reference.conf"
            conf.comment.brief = "Aggregated standard configuration of merged reference.conf files"
            commentedConfigs += conf
        }

        for (config <- commentedConfigs) {
            config.collapse()
        }

        commentedConfigs.toSeq
    }

    /**
     * Handles the frame around parsing the configuration file.
     * Also checks if the config files are in an allowed HOCON formats to prevent endless loops.
     * @param filepath accepts the full path to a valid HOCON config file.
     * @return returns the parsed config as a ConfigNode.
     */
    def ParseComments(filepath: Path, rootDirectory: Path): ConfigObject = {
        // This prevents the Parser from parsing a file without valid syntax
        ConfigFactory.load(filepath.toString)

        val cp = new CommentParser
        cp.parseFile(filepath, rootDirectory)
    }

    /**
     * Inner class of the Comment parser Wrapper
     * This class handles the parsing process itself
     */
    private class CommentParser {
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
                    if (line.startsWith("}")) {
                        // Found the closing bracket of the object. Remove the closing bracket and stop parsing the object
                        line = line.stripPrefix("}")
                        break()

                    } else if (line != "") {
                        // If none of the options above apply and the line is NOT empty (in which case load the next line and ignore this)
                        // What follows now is part of the content of the object
                        // Objects are Key Value pairs, so parsing these is a two stage job: Separating Key and value and then parsing the value

                        // 1. Separating Key and value
                        // In JSON, Keys and values are separated with ':'. HOCON allows substituting ':' with '=' and also allows ommitting these symbols when using a '{' or '[' to open an object/list afterwards
                        // Finding first instance of these symbols
                        // TerminatingIndex is the index of the symbol that terminates the key.
                        val terminatingChars = Set(':', '=', '{', '[')
                        val terminatingIndex = findFirstIndexOfAnyChar(terminatingChars, line)

                        // Splitting the key from the string (while splitting of the ':' or '=' as they are not needed anymore
                        currentKey = line.substring(0, terminatingIndex - 1).trim.stripPrefix("\"").stripSuffix("\"")
                        line = line.substring(terminatingIndex).trim.stripPrefix(":").stripPrefix("=").trim

                        // Evaluating the type of value
                        if (line.startsWith("{")) {
                            // Case: Value is an object
                            currentvalue = parseObject(nextComment)
                        } else if (line.startsWith("[")) {
                            // Case: Value is a list
                            currentvalue = parseList(nextComment)
                        } else {
                            // Case: Value is an entry
                            currentvalue = parseEntry(nextComment)
                        }

                        // Reset next comment
                        nextComment = ListBuffer[String]()

                        // Json Keys are split using a ",". This is not necessary, but tolerated in HOCON syntax
                        line = line.stripPrefix(",").trim

                        // Adding the new Key, Value pair to the Map
                        entries += ((currentKey, currentvalue))
                    }

                    // Proceed with the next line if the current one was fully parsed
                    if (line.trim == "" && iterator.hasNext) {
                        line = iterator.next().trim
                    }
                }
            }

            // If there is a comment directly behind the closing bracket of the object, add it to comments too.
            if (line.startsWith("#") || line.startsWith("//")) {
                currentComment += line.stripPrefix("#").stripPrefix("//").trim
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

            if (line.startsWith("\"\"\"")) {
                // Case: line starts with a triple quoted string (These allow for multi-line values, so the line end does not necessarily terminate the value
                line = line.stripPrefix("\"\"\"").trim
                val valueBuilder = new StringBuilder
                var index = line.indexOf("\"\"\"")
                if (index >= 0) {
                    // The value is a single line value
                    valueBuilder ++= line.substring(0, index)
                    line = line.substring(index).stripPrefix("\"\"\"").trim
                } else {
                    // The value is a multi line value
                    valueBuilder ++= s"$line \n"
                    breakable {
                        while (iterator.hasNext) {
                            line = iterator.next().trim
                            index = line.indexOf("\"\"\"")
                            if (index >= 0) {
                                valueBuilder ++= s"${line.substring(0, index)} \n"
                                line = line.stripPrefix(line.trim.substring(
                                    0,
                                    index
                                )).stripPrefix("\"\"\"").trim
                                break()
                            } else {
                                valueBuilder ++= s"$line \n"
                            }
                        }
                    }
                }
                value = valueBuilder.toString
            } else if (line.startsWith("\"")) {
                // Case: line starts with a double quoted string
                line = line.stripPrefix("\"").trim
                // A '\' can escape a quote. Thus we need to exclude that from the terminating Index
                var index = line.indexOf('\"')
                breakable(while (index != -1) {
                    if (index == 0 || line(index - 1) != '\\') {
                        break()
                    } else {
                        index = line.indexOf('\"', index + 1)
                    }
                })

                value = line.substring(0, index).trim
                line = line.stripPrefix(value).trim.stripPrefix("\"").trim
            } else if (line.startsWith("\'")) {
                // Case: line starts with a single quoted string
                line = line.stripPrefix("\'").trim
                // A '\' can escape a quote. Thus we need to exclude that from the terminating Index
                var index = line.indexOf('\'')
                breakable(while (index != -1) {
                    if (index == 0 || line(index - 1) != '\\') {
                        break()
                    } else {
                        index = line.indexOf('\'', index + 1)
                    }
                })

                value = line.substring(0, index).trim
                line = line.stripPrefix(value).trim.stripPrefix("\'").trim
            } else {
                // Case: Line starts with an unquoted string
                // There are two ways of terminating an unquoted string
                // Option 1: The value is inside of a pattern that has other control structures
                val terminatingChars = Set(',', ']', '}', ' ')
                val terminatingIndex = findFirstIndexOfAnyChar(terminatingChars, line)

                if (terminatingIndex > 0) {
                    value = line.substring(0, terminatingIndex).trim
                    line = line.stripPrefix(value).trim
                } else {
                    // Option 2: The end of the line
                    value = line
                    line = ""
                }
            }

            currentComment += getSingleLineComment
            ConfigEntry(value, DocumentationComment.fromString(currentComment))
        }

        /**
         * Method responsible to parse List-Type Nodes.
         * @param currentComment assings previously parsed comment to this Node. This is necessary as most comments appear before the opening bracket of an object (Which identifies it as an object).
         * @return returns the fully parsed entry.
         */
        private def parseList(currentComment: ListBuffer[String]): ConfigList = {
            line = line.stripPrefix("[").trim
            // Creating necessary variables
            val value = new ListBuffer[ConfigNode]
            var nextComment = ListBuffer[String]()

            breakable {
                while (iterator.hasNext || line.nonEmpty) {
                    nextComment = parseComments()
                    if (line.startsWith("{")) {
                        // Case: The following symbol opens an object
                        value += parseObject(nextComment)
                        line = line.stripPrefix(",").trim
                    } else if (line.startsWith("[")) {
                        // Case: The following symbol opens a list
                        value += parseList(nextComment)
                        line = line.stripPrefix(",").trim
                    } else if (
                        line.startsWith("]") || (line.startsWith(",") && line.stripPrefix(",").trim.startsWith("]"))
                    ) {
                        // Case: The following symbol closes the list
                        line = line.stripPrefix(",").trim.stripPrefix("]").trim
                        break()
                    } else if (line != "") {
                        // Case: The following symbol is an entry
                        value += parseEntry(nextComment)
                        line = line.stripPrefix(",").trim
                    }

                    if (line == "" && iterator.hasNext) {
                        // Load next line when done
                        line = iterator.next().trim
                    }
                }
            }
            currentComment += getSingleLineComment

            // Finish
            ConfigList(value, DocumentationComment.fromString(currentComment))
        }

        /**
         * Method that finds the first instance of a character out of a character set.
         * @param characters accepts the selection of characters that is supposed to be looked for.
         * @param string accepts the string that is to be searched.
         * @return returns the lowest index of any character out of the character set that occurs in the string.
         * @return returns -1 if none of the characters appear in the string.
         */
        private def findFirstIndexOfAnyChar(characters: Set[Char], string: String): Int = {
            string.indexWhere(characters.contains)
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
            while (line.startsWith("#") || line.startsWith("//") || line == "") {
                if (line != "") comment += getSingleLineComment
                line = iterator.next().trim
            }
            comment
        }

        /**
         * Adds a single line of Comment to the raw Comment string if the line has the comment flags
         * @return returns an empty string if the next line is not a Comment. Returns the Comment without the comment flags if the line is a comment.
         */
        private def getSingleLineComment: String = {
            if (line.startsWith("#") || line.startsWith("//")) {
                // Add the comment in the same line of the list as well
                val currentComment = line.stripPrefix("#").stripPrefix("//").trim
                line = ""
                return currentComment
            }
            ""
        }
    }
}
