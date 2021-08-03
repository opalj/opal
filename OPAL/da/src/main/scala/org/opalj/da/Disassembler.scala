/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import java.io.File
import java.nio.file.Files

import org.apache.commons.text.similarity.LevenshteinDistance.{getDefaultInstance => getLevenshteinDistance}

import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.{Error => ErrorLogLevel}
import org.opalj.bytecode.JRELibraryFolder

/**
 * Disassembles the specified class file(s).
 *
 * @author Michael Eichberg
 */
object Disassembler {

    OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, ErrorLogLevel))

    private final val Usage = {
        "Usage: java …Disassembler \n"+
            "       [-help will print this help and terminate.]\n"+
            "       [-o <File> the name of the file to which the generated html page should be written]\n"+
            "       [-open the generated html page will be opened in a browser]\n"+
            "       [-source <File> a class or jar file or a directory containing jar or class files;\n"+
            "                       if no source files/folders are specified the current folder will be\n"+
            "                       searched for class files]*\n"+
            "       [-sourceJDK the current JDK/JRE is added as a source folder]\n"+
            "       [-noDefaultCSS the generated html page will have no CSS styling]\n"+
            "       [-noMethodsFilter the generated html page will have no embedded means to filter methods\n"+
            "                         (as a whole, the file will not contain any JavaScript code)]\n"+
            "       [-noHeader the generated output will have no header; \n"+
            "                  the top level element will be <div class=\"class_file\">...</div>\n"+
            "                  (automatically activates \"-noMethodsFilter\" and \"-noDefaultCSS\")]\n"+
            "       [-css <Source> the path (URL) of a CSS file (\".csss\")\n"+
            "                      which will be referenced from the generated HTML page]\n"+
            "       [-js <Source> the path (URL) of a JavaScript file (\".js\")\n"+
            "                     which will be referenced from the generated HTML page]\n"+
            "       [-showProgress shows the progress when searching for the class file]\n"+
            "       [<ClassName> name of the class for which we want to create the HTML page;\n"+
            "                    if not specified the first class that is found on the given path is taken;\n"+
            "                    this is particularly useful if the source identifies a particular \".class\" file]\n"+
            "\n"+
            "Note:   \n       If no parameters are specified, the first class file found in the current folder\n"+
            "       or any subfolder of it will be disassembled.\n"+
            "Example:\n       java …Disassembler -source /Library/jre/lib/rt.jar java.util.ArrayList"
    }

    def handleError(error: String, showUsage: Boolean = true): Nothing = {
        Console.err.println("Error: "+error)
        if (showUsage) Console.out.println(Usage)
        sys.exit(1)
    }

    def main(args: Array[String]): Unit = {
        // OPTIONS
        var toStdOut = true
        var toFile: Option[String] = None
        var openHTMLFile: Boolean = false
        var sources: List[String] = List.empty
        var noDefaultCSS: Boolean = false
        var noHeader: Boolean = false
        var noMethodsFilter: Boolean = false
        var css: Option[String] = None
        var js: Option[String] = None
        var showProgress: Boolean = false
        var className: String = null

        // PARSING PARAMETERS
        var i = 0
        def readNextArg(): String = {
            i += 1
            if (i < args.length) {
                args(i)
            } else {
                handleError(args.mkString("missing argument: ", " ", ""))
            }
        }
        while (i < args.length) {
            args(i) match {
                case "-help" | "--help" => { Console.out.println(Usage); return }
                case "-o"               => { toFile = Some(readNextArg()); toStdOut = false }
                case "-open"            => { openHTMLFile = true; toStdOut = false }
                case "-noDefaultCSS"    => noDefaultCSS = true
                case "-noMethodsFilter" => noMethodsFilter = true
                case "-noHeader"        => { noHeader = true; noMethodsFilter = true; noDefaultCSS = true }
                case "-css"             => css = Some(readNextArg())
                case "-js"              => js = Some(readNextArg())
                case "-source"          => sources ::= readNextArg()
                case "-sourceJDK"       => sources ::= JRELibraryFolder.toString
                case "-showProgress"    => showProgress = true
                case cName              => className = cName.replace('/', '.')
            }
            i += 1
        }

        // VALIDATING PARAMETERS
        if (noHeader) {
            if (css.nonEmpty) handleError("specifying -noHeader and a css file is not supported")
            if (js.nonEmpty) handleError("specifying -noHeader and a js file is not supported")
        }

        if (sources.isEmpty) sources = List(System.getProperty("user.dir"))
        val sourceFiles = sources map { src =>
            val f = new File(src)
            if (!f.exists()) handleError("file does not exist: "+src, false)
            if (!f.canRead) handleError("cannot read: "+src, false)
            f
        }

        val classFileFilter =
            if (className == null)
                (cf: ClassFile) => true // just take the first one...
            else
                (cf: ClassFile) => cf.thisType.asJava == className

        val (classFile, source) =
            ClassFileReader.findClassFile(
                sourceFiles, (if (showProgress) println else (f) => {}),
                classFileFilter, (cf: ClassFile) => cf.thisType.asJava
            ) match {
                case Left(cfSource) => cfSource
                case Right(altClassNames) =>
                    if (altClassNames.isEmpty) {
                        handleError(sources.mkString("cannot find class files in: ", ", ", ""))
                    } else {
                        val allClassNames: List[(Int, String)] =
                            altClassNames.map { cf =>
                                (getLevenshteinDistance()(className, cf).intValue, cf)
                            }.toList
                        val mostRelated = allClassNames.sortWith((l, r) => l._1 < r._1).map(_._2).take(15)
                        val ending = if (mostRelated.length > 15) ", ...)" else ")"
                        val messageHeader = "can't find: "+className
                        val message = mostRelated.mkString(s"$messageHeader (similar: ", ", ", ending)
                        handleError(message, false)
                    }
            }

        val classNameAsFileName: String = org.opalj.io.sanitizeFileName(classFile.thisType.asJava)

        val targetFile: Option[File] =
            if (openHTMLFile) {
                if (toFile.isEmpty)
                    Some(File.createTempFile(classNameAsFileName, ".html"))
                else {
                    val f = new File(toFile.get)
                    if (f.exists() && !f.canWrite) handleError("cannot update: "+f)
                    Some(f)
                }
            } else if (toFile.isDefined) {
                val f = new File(toFile.get)
                if (f.exists() && !f.canWrite) handleError("cannot update: "+f)
                Some(f)
            } else {
                None
            }

        // FINAL PROCESSING
        val htmlCSS = if (noDefaultCSS) None else Some(ClassFile.TheCSS)
        val xHTML =
            if (noHeader)
                classFile.classFileToXHTML(Some(source)).toString
            else
                classFile.toXHTML(Some(source), htmlCSS, css, js, !noMethodsFilter).toString

        targetFile match {
            case Some(f) =>
                Files.write(f.toPath, xHTML.toString.getBytes("UTF-8"))
                println("wrote: "+f)
                if (openHTMLFile) org.opalj.io.open(f)
            case None =>
                Console.out.println(xHTML)
        }

    }
}
