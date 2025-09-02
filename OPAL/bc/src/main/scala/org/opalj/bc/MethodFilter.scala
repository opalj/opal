/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bc

import scala.language.postfixOps

import java.io.File

import org.rogach.scallop.ScallopConf

import org.opalj.cli.ClassPathArg
import org.opalj.cli.MethodNameArg
import org.opalj.cli.OPALCommandLineConfig
import org.opalj.da.*
import org.opalj.io.write
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * Command-line application which writes out some class files where some methods are filtered.
 *
 * @author Michael Eichberg
 */
object MethodFilter {

    protected class MethodFilterConfig(args: Array[String]) extends ScallopConf(args) with OPALCommandLineConfig {
        val description =
            "Writes out some class files where some methods are filtered (prefix method name with + to keep or - to remove the method)"

        args(
            ClassPathArg !,
            MethodNameArg !
        )
    }

    implicit val logContext: LogContext = GlobalLogContext

    def main(args: Array[String]): Unit = {
        val analysisConfig = new MethodFilterConfig(args)

        val modeClassMethod = analysisConfig(MethodNameArg).map { case (modeAndClassName, methodName) =>
            (modeAndClassName.charAt(0) == '+', modeAndClassName.substring(1), methodName)
        }

        for (cpEntry <- analysisConfig(ClassPathArg)) {
            val classFiles = ClassFileReader.ClassFiles(cpEntry).map(_._1)
            if (classFiles.isEmpty) {
                OPALLogger.error("setup", s"no class files found in ${args(0)}")
            } else {
                for {
                    (mode, className, methodName) <- modeClassMethod
                } {
                    classFiles.filter(_.thisType.asJava == className) foreach { cf =>
                        val filteredMethods = cf.methods.filter { m =>
                            implicit val cp: Constant_Pool = cf.constant_pool
                            val matches = m.name == methodName
                            if (mode)
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
    }
}
