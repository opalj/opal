/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.io.File

import com.typesafe.config.ConfigValueFactory

import org.opalj.io.writeAndOpen
import org.opalj.br.analyses.Project
import org.opalj.ai.domain
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.log.GlobalLogContext
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.OPALLogger
import org.opalj.log.{Error => ErrorLogLevel}
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.BaseConfig
import org.opalj.br.reader.InvokedynamicRewriting
import org.opalj.br.reader.DynamicConstantRewriting

/**
 * Creates the three-address representation for some method(s) and prints it to `std out` or writes
 * it to a file.
 *
 * @example To convert all files of a project to the AI based three-address code, you can use:
 * {{{
 * import org.opalj.io.write
 * import org.opalj.util.PerformanceEvaluation.time
 * import org.opalj.tac._
 * val f = new java.io.File("OPAL/bi/target/scala-2.12/resource_managed/test/ai.jar")
 * val p = org.opalj.br.analyses.Project(f)
 * var i = 0
 * val errors = time {
 *   p.parForeachMethodWithBody(parallelizationLevel=32){ mi =>
 *     val TACode(code,cfg,ehs,_) = org.opalj.tac.TACAI(p,mi.method)()
 *     val tac = ToTxt(code, Some(cfg))
 *     val fileNamePrefix = mi.classFile.thisType.toJava+"."+mi.method.name
 *     val file = write(tac, fileNamePrefix, ".tac.txt")
 *     i+= 1
 *     println(i+":"+file)
 *   }
 * }(t => println("transformation time: "+t.toSeconds))
 * if(errors.nonEmpty) println(errors.mkString("\n"))
 * }}}
 *
 * @author Michael Eichberg
 */
object TAC {

    OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, ErrorLogLevel))

    def handleError(message: String): Nothing = {
        Console.err.println(error(message))
        sys.exit(-1)
    }

    def error(message: String): String = s"Error: $message \n$usage"

    def usage: String = {
        "Usage: java …TAC \n"+
            "-cp <JAR file/Folder containing class files> OR -JDK\n"+
            "[-libcp <JAR file/Folder containing library class files>]* (generally required to get precise/correct type information\n"+
            "[-domainValueInformation] (prints detailed information about domain values)\n"+
            "[-class <class file name>] (filters the set of classes)\n"+
            "[-method <method name/signature using Java notation>] (filters the set of methods)\n"+
            "[-naive] (the naive representation is generated)\n"+
            "[-domain <class name of the domain>]\n"+
            "[-cfg] (print control-flow graph)\n"+
            "[-open] (the generated representations will be written to disk and opened)\n"+
            "[-toString] (uses the \"toString\" method to print the object graph)\n"+
            "[-performConstantPropagation] (performs constant propagation)\n"+
            "[-rewriteInvokeDynamic] (rewrites InvokeDynamic bytecode instructions)\n"+
            "[-rewriteDynamicConstants] (rewrites dynamic constants)\n+"+
            "[-rewriteAll] (rewrites InvokeDynamicInstructions and dynamic constants)"+
            "Example:\n\tjava …TAC -cp /Library/jre/lib/rt.jar -class java.util.ArrayList -method toString"
    }

    def main(args: Array[String]): Unit = {

        // Parameters:
        var cp: List[String] = Nil
        var libcp: List[String] = Nil
        var doOpen: Boolean = false
        var className: Option[String] = None
        var methodSignature: Option[String] = None
        var naive: Boolean = false
        var toString: Boolean = false
        var domainName: Option[String] = None
        var printCFG: Boolean = false
        var performConstantPropagation: Boolean = false
        var rewriteInvokeDynamic = false
        var rewriteDynamicConstants = false

        // PARSING PARAMETERS
        var i = 0

        def readNextArg(): String = {
            i += 1
            if (i < args.length) {
                args(i)
            } else {
                handleError(s"missing argument: ${args(i - 1)}")
            }
        }

        while (i < args.length) {
            args(i) match {
                case "-naive" =>
                    naive = true
                    if (domainName.nonEmpty) handleError("-naive and -domain cannot be combined")

                case "-domainValueInformation" => DUVar.printDomainValue = true
                case "-domain" =>
                    domainName = Some(readNextArg())
                    if (naive) handleError("-naive and -domain cannot be combined")

                case "-JDK"                        => cp ::= JRELibraryFolder.toString
                case "-cp"                         => cp ::= readNextArg()
                case "-libcp"                      => libcp ::= readNextArg()
                case "-cfg"                        => printCFG = true
                case "-open"                       => doOpen = true
                case "-class"                      => className = Some(readNextArg())
                case "-method"                     => methodSignature = Some(readNextArg())
                case "-toString"                   => toString = true
                case "-performConstantPropagation" => performConstantPropagation = true
                case "-rewriteInvokeDynamic"       => rewriteInvokeDynamic = true
                case "-rewriteDynamicConstants"    => rewriteDynamicConstants = true
                case "-rewriteAll" =>
                    rewriteInvokeDynamic = true
                    rewriteDynamicConstants = true

                case unknown => handleError(s"unknown parameter: $unknown")
            }
            i += 1
        }

        if (cp == null) {
            handleError("missing parameters")
        }

        val config = BaseConfig.withValue(
            InvokedynamicRewriting.InvokedynamicRewritingConfigKey,
            ConfigValueFactory.fromAnyRef(rewriteInvokeDynamic)
        ).withValue(
                DynamicConstantRewriting.RewritingConfigKey,
                ConfigValueFactory.fromAnyRef(rewriteDynamicConstants)
            )

        val sourceFiles = cp.map(new File(_)).toArray
        val sourceLibFiles = libcp.map(new File(_)).toArray
        val project = Project(sourceFiles, sourceLibFiles, GlobalLogContext, config)
        if (project.projectMethodsCount == 0) {
            handleError(s"no methods found: $cp")
        }

        val ch = project.classHierarchy
        for {
            cf <- project.allClassFiles
            if className.isEmpty || className.get == cf.thisType.toJava
        } {
            val methodsAsTAC = new StringBuilder()

            for {
                m <- cf.methods
                mSig = (if (m.isStatic) "static " else "") + m.descriptor.toJava(m.name)
                if methodSignature.isEmpty || mSig.contains(methodSignature.get)
                code <- m.body
            } {
                val (tac: String, cfg: String, ehs: Option[String]) =
                    if (naive) {
                        val tac @ TACode(params, code, _, cfg, ehs) =
                            TACNaive(m, ch, AllNaiveTACodeOptimizations)
                        if (toString) Console.out.println(m.toJava(tac.toString))

                        (
                            ToTxt(params, code, cfg, skipParams = true, true, true).mkString("\n"),
                            tacToDot(code, cfg),
                            if (ehs.nonEmpty)
                                Some(ehs.mkString("\n\n      /*\n      ", "\n      ", "\n      */"))
                            else
                                None
                        )
                    } else {
                        val d: Domain with RecordDefUse = if (domainName.isEmpty) {
                            new domain.l1.DefaultDomainWithCFGAndDefUse(project, m)
                        } else {
                            // ... "org.opalj.ai.domain.l0.BaseDomainWithDefUse"
                            Class.
                                forName(domainName.get).asInstanceOf[Class[Domain with RecordDefUse]].
                                getConstructor(classOf[Project[_]], classOf[Method]).
                                newInstance(project, m)
                        }
                        // val d = new domain.l0.BaseDomainWithDefUse(project, classFile, method)
                        val aiResult = BaseAI(m, d)
                        val classHierarchy = project.classHierarchy
                        val tac @ TACode(params, code, _, cfg, ehs) =
                            TACAI(m, classHierarchy, aiResult, performConstantPropagation)(Nil)
                        if (toString) Console.out.println(m.toJava(tac.toString))

                        (
                            ToTxt(params, code, cfg, skipParams = false, true, true).mkString("\n"),
                            tacToDot(code, cfg),
                            if (ehs.nonEmpty)
                                Some(ehs.mkString("\n\n      /*\n      ", "\n      ", "\n      */"))
                            else
                                None
                        )
                    }

                methodsAsTAC.append(mSig)
                methodsAsTAC.append("{\n")
                methodsAsTAC.append(tac)
                ehs.map(methodsAsTAC.append)
                if (printCFG) {
                    if (doOpen) {
                        Console.println(
                            "wrote cfg to: "+writeAndOpen(cfg, m.toJava, ".cfg.gv")
                        )
                    } else {
                        methodsAsTAC.append("\n/* - CFG")
                        methodsAsTAC.append(cfg)
                        methodsAsTAC.append("*/\n")
                    }
                }
                methodsAsTAC.append("\n}\n\n")
            }

            if (doOpen) {
                val prefix = cf.thisType.toJava
                val suffix = if (naive) ".naive-tac.txt" else ".ai-tac.txt"
                val targetFile = writeAndOpen(methodsAsTAC.toString(), prefix, suffix)
                Console.println("wrote tac code to: "+targetFile)
            } else {
                Console.println(methodsAsTAC.toString())
            }
        }
    }
}
