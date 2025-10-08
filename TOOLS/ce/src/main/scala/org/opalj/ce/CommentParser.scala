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

import com.typesafe.config.ConfigFactory

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger

/**
 * Parses commented config files for the Configuration Explorer.
 */
object CommentParser {
    /**
     * Made to parse multiple Configuration Files in bulk.
     * Used in combination with the file Locator to locate and parse all config files of a project.
     * @param filepaths accepts a list of full paths to the HOCON config files that shall be parsed.
     * @param rootDirectory the project root directory, to allow for relative paths.
     * @return is a Seq of the parsed configuration files, paired with the path they originate from.
     */
    def iterateConfigs(filepaths: Iterable[Path], rootDirectory: Path): Seq[ConfigObject] = {
        val commentedConfigs = filepaths.map(filepath => parseComments(filepath, rootDirectory)).toList

        // Merge all config files named "reference.conf"
        val (mergingConfigs, otherConfigs) = commentedConfigs.partition(_.comment.label.endsWith("reference.conf"))

        val mergedReferenceConfOpt = if (mergingConfigs.nonEmpty) {
            val mergedConfig =
                mergingConfigs.foldLeft(ConfigObject(
                    mutable.Map[String, ConfigNode](),
                    new DocumentationComment(
                        "reference.conf",
                        "Aggregated standard configuration of merged reference.conf files",
                        Seq(),
                        "",
                        Seq()
                    )
                )) {
                    (accumulatedConfig, mergingConfig) => accumulatedConfig.merge(mergingConfig); accumulatedConfig
                }
            Some(mergedConfig)
        } else {
            None
        }

        val finalConfigs = otherConfigs ++ mergedReferenceConfOpt
        finalConfigs.foreach(config => config.collapse())

        finalConfigs
    }

    /**
     * Handles the frame around parsing the configuration file.
     * Also checks if the config files are in an allowed HOCON formats to prevent endless loops.
     * @param filePath accepts the full path to a valid HOCON config file.
     * @param rootDirectory the project root directory, to allow for relative paths.
     * @return returns the parsed config as a ConfigNode.
     */
    def parseComments(filePath: Path, rootDirectory: Path): ConfigObject = {
        // This prevents the Parser from parsing a file without valid syntax
        ConfigFactory.load(filePath.toString)

        OPALLogger.info("Configuration Explorer", s"Parsing: $filePath")(GlobalLogContext)

        Using.resource(Source.fromFile(filePath.toFile)) { source =>
            new HOCONParser(source.getLines()).parseFile(filePath, rootDirectory)
        }
    }

    /**
     * This class handles the parsing process itself
     */
    private class HOCONParser(var configLines: Iterator[String]) {
        private var line = ""

        /**
         * parseComments initiates the parsing process.
         * A ConfigNode can consist out of 3 possible types that are parsed differently: Objects, Lists and Entries.
         * The source node of a config file always is an object.
         * During parsing, the Parser will iterate the file and sort it into the ConfigNode structure.
         * Since the Nodes can be nested and there can be multiple Nodes in one line, the Parser needs to examine most control structures like a stream and not linewise.
         * @param filePath accepts the path to a valid HOCON file.
         * @param rootDirectory the project root directory, to allow for relative paths.
         * @return returns the fully parsed file as a configObject.
         */
        def parseFile(filePath: Path, rootDirectory: Path): ConfigObject = {
            // Parse initial Comments
            val initialComment = ListBuffer[String]()
            initialComment += ("@label " + rootDirectory.relativize(filePath))
            parseComments(initialComment)
            parseObject(initialComment)
        }

        val objectKeyTerminatingChars = Set(':', '=', '{', '[')

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
                while (configLines.hasNext || line.nonEmpty) {
                    parseComments(nextComment)
                    if (line.startsWith("}")) {
                        // Found the closing bracket of the object. Remove the closing bracket and stop parsing the object
                        line = line.stripPrefix("}")
                        break();
                    } else if (line.nonEmpty) {
                        // What follows now is part of the content of the object
                        // Objects are Key Value pairs, so parsing them is a two stage job:
                        // Separating Key and value and then parsing the value

                        // 1. Separating Key and value
                        // In JSON, Keys and values are separated with ':'. HOCON allows substituting ':' with '=' and
                        // also allows ommitting these symbols when using a '{' or '[' to open an object/list afterward
                        // Finding first instance of these symbols
                        // TerminatingIndex is the index of the symbol that terminates the key.
                        val terminatingIndex = line.indexWhere(objectKeyTerminatingChars.contains)

                        // Splitting the key from the string (while splitting of the ':' or '=' as they are not needed anymore
                        currentKey = line.substring(0, terminatingIndex - 1).trim.stripPrefix("\"").stripSuffix("\"")
                        line = line.substring(terminatingIndex).stripPrefix(":").stripPrefix("=").trim

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

                        // Json Keys are split using a ",". This is not necessary, but tolerated in HOCON syntax
                        line = line.stripPrefix(",").trim

                        // If there is a comment directly behind the comma, add it to comments too.
                        getSingleLineComment.foreach { comment =>
                            currentvalue.comment =
                                currentvalue.comment.mergeComment(DocumentationComment.fromString(Seq(comment)))
                        }

                        // Reset next comment
                        nextComment = ListBuffer[String]()

                        // Adding the new Key, Value pair to the Map
                        entries += ((currentKey, currentvalue))
                    }

                    // Proceed with the next line if the current one was fully parsed
                    while (line.isEmpty && configLines.hasNext) {
                        line = configLines.next().trim
                    }
                }
            }

            // If there is a comment directly behind the closing bracket of the object, add it to comments too.
            currentComment ++= getSingleLineComment

            // Return the finished ConfigObject
            ConfigObject(entries, DocumentationComment.fromString(currentComment))
        }

        val stringTerminatingChars = Set(',', ']', '}', ' ')

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
                line = line.stripPrefix("\"\"\"")
                val valueBuilder = new StringBuilder
                var index = line.indexOf("\"\"\"")
                if (index >= 0) {
                    // The value is a single line value
                    valueBuilder ++= line.substring(0, index)
                    line = line.substring(index).stripPrefix("\"\"\"").trim
                } else {
                    // The value is a multi line value
                    while (index == -1 && configLines.hasNext) {
                        valueBuilder ++= s"$line\n"
                        line = configLines.next()
                        index = line.indexOf("\"\"\"")
                    }
                    if (index != -1) {
                        valueBuilder ++= s"${line.substring(0, index)}"
                        line = line.substring(index).stripPrefix("\"\"\"").trim
                    } else {
                        valueBuilder ++= s"$line\n"
                    }
                }
                value = valueBuilder.toString
            } else if (line.startsWith("\"")) {
                // Case: line starts with a double-quoted string
                line = line.stripPrefix("\"").trim
                // A '\' can escape a quote. Thus, we need to exclude that from the terminating Index
                var index = line.indexOf('"')
                while (index > 0 && line(index - 1) == '\\') {
                    index = line.indexOf('"', index + 1)
                }

                value = line.substring(0, index).replace("\\\"", "\"")
                line = line.substring(index + 1).trim
            } else if (line.startsWith("\'")) {
                // Case: line starts with a single quoted string
                line = line.stripPrefix("\'").trim
                // A '\' can escape a quote. Thus, we need to exclude that from the terminating Index
                var index = line.indexOf('\'')
                while (index > 0 && line(index - 1) == '\\') {
                    index = line.indexOf('\'', index + 1)
                }

                value = line.substring(0, index).replace("\\'", "'")
                line = line.substring(index + 1).trim
            } else {
                // Case: Line starts with an unquoted string
                // There are two ways of terminating an unquoted string
                // Option 1: The value is inside a pattern that has other control structures
                val terminatingIndex = line.indexWhere(stringTerminatingChars.contains)

                if (terminatingIndex > 0) {
                    value = line.substring(0, terminatingIndex).trim
                    line = line.stripPrefix(value).trim
                } else {
                    // Option 2: The end of the line
                    value = line
                    line = ""
                }
            }

            currentComment ++= getSingleLineComment
            ConfigEntry(value, DocumentationComment.fromString(currentComment))
        }

        /**
         * Method responsible to parse List-Type Nodes.
         * @param currentComment assigns previously parsed comment to this Node. This is necessary as most comments appear before the opening bracket of an object (Which identifies it as an object).
         * @return returns the fully parsed entry.
         */
        private def parseList(currentComment: ListBuffer[String]): ConfigList = {
            line = line.stripPrefix("[").trim
            // Creating necessary variables
            val value = new ListBuffer[ConfigNode]
            var nextComment = ListBuffer[String]()

            breakable {
                while (configLines.hasNext || line.nonEmpty) {
                    nextComment = parseComments()
                    if (line.startsWith("]") || (line.startsWith(",") && line.stripPrefix(",").trim.startsWith("]"))) {
                        // Case: The following symbol closes the list
                        line = line.stripPrefix(",").trim.stripPrefix("]").trim
                        break();
                    } else if (line.startsWith("{")) {
                        // Case: The following symbol opens an object
                        value += parseObject(nextComment)
                    } else if (line.startsWith("[")) {
                        // Case: The following symbol opens a list
                        value += parseList(nextComment)
                    } else if (line.nonEmpty) {
                        // Case: The following symbol is an entry
                        value += parseEntry(nextComment)
                    }
                    line = line.stripPrefix(",").trim

                    // If there is a comment directly behind the comma, add it to comments too.
                    getSingleLineComment.foreach { comment =>
                        value.last.comment =
                            value.last.comment.mergeComment(DocumentationComment.fromString(Seq(comment)))
                    }

                    while (line.isEmpty && configLines.hasNext) {
                        // Load next line when done
                        line = configLines.next().trim
                    }
                }
            }
            currentComment ++= getSingleLineComment

            ConfigList(value, DocumentationComment.fromString(currentComment))
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
         * Gets all comments until the next non-comment entry and writes them into an existing ListBuffer.
         * @param comment Accepts the ListBuffer that the following comment should be added to.
         * @return Returns the existing ListBuffer, but with the content of the comment added to it.
         */
        private def parseComments(comment: ListBuffer[String]): ListBuffer[String] = {

            while (line.startsWith("#") || line.startsWith("//") || line.isEmpty) {
                comment ++= getSingleLineComment
                line = configLines.next().trim
            }
            comment
        }

        /**
         * Adds a single line of Comment to the raw Comment string if the line has the comment flags
         * @return Returns the Comment text if the line is a comment. Else, returns an empty string.
         */
        private def getSingleLineComment: Option[String] = {
            if (line.startsWith("#")) {
                // Add the comment in the same line of the list as well
                val currentComment = line.stripPrefix("#").stripPrefix("//").trim
                line = ""
                Some(currentComment)
            } else {
                None
            }
        }
    }
}
