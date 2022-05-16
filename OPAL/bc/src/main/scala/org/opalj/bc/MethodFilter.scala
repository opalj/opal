/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bc

import java.io.File

import org.opalj.io.write
import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.da._

/**
 * Command-line application which writes out some class files where some methods are filtered.
 *
 * @author Michael Eichberg
 */
object MethodFilter {

    implicit val logContext: LogContext = GlobalLogContext

    private final val Usage =
        "Usage: java …MethodFilter \n"+
            "(1) <JAR file containing class files>\n"+
            "(2) <name of the class in binary notation (e.g. java/lang/Object>\n"+
            "(3) (+|-)<name of methods to keep/remove>\n"+
            "Example:\n\tjava …Disassembler /Library/jre/lib/rt.jar java/util/ArrayList +toString"

    def main(args: Array[String]): Unit = {
        if (args.length != 3) {
            println(Usage)
            sys.exit(-1)
        }

        val jarName = args(0)
        val className = args(1)
        val methodName = args(2).substring(1)
        val keepMethod = args(2).charAt(0) == '+'
        val classFiles = ClassFileReader.ClassFiles(new File(jarName)).map(_._1)
        if (classFiles.isEmpty) {
            OPALLogger.error("setup", s"no class files found in ${args(0)}")
        } else {
            classFiles.filter(_.thisType.asJVMType == className) foreach { cf =>
                val filteredMethods = cf.methods.filter { m =>
                    implicit val cp = cf.constant_pool
                    val matches = m.name == methodName
                    if (keepMethod)
                        matches
                    else
                        !matches
                }
                val filteredCF = cf.copy(methods = filteredMethods)
                val path = new File(s"${cf.thisType}.class").toPath
                write(Assembler(filteredCF), path)
                OPALLogger.info("info", s"created new class file: $path")
            }
        }
    }
}
