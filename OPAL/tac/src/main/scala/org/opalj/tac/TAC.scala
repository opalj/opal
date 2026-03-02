/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.io.File

import org.rogach.scallop.flagConverter

import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.cli.AIBasedCommandLineConfig
import org.opalj.ai.cli.DomainArg
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.CFGArg
import org.opalj.cli.ClassNameArg
import org.opalj.cli.NaiveTACArg
import org.opalj.cli.PartialSignatureArg
import org.opalj.cli.PlainArg
import org.opalj.cli.TempFileArg
import org.opalj.cli.VerboseArg
import org.opalj.io.writeAndOpen
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.Error as ErrorLogLevel
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger

/**
 * Creates the three-address representation for some method(s) and prints it to `std out` or writes
 * it to a file.
 *
 * @example To convert all files of a project to the AI based three-address code, you can use:
 * {{{
 * import org.opalj.io.write
 * import org.opalj.util.PerformanceEvaluation.time
 * import org.opalj.tac.*
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
object TAC extends ProjectsAnalysisApplication {

    protected class TACConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with AIBasedCommandLineConfig {
        val description = "Creates the three-address representation for some method(s)"

        private val constantPropagationArg = new PlainArg[Boolean] {
            override val name: String = "performConstantPropagation"
            override val description: String = "Perform constant propagation"
            override val defaultValue: Option[Boolean] = Some(false)
        }

        private val toStringArg = new PlainArg[Boolean] {
            override val name: String = "useToString"
            override val description: String = "Use the \"toString\" method to print the object graph"
            override val defaultValue: Option[Boolean] = Some(false)
        }

        args(
            TempFileArg,
            ClassNameArg,
            PartialSignatureArg,
            VerboseArg,
            NaiveTACArg,
            CFGArg,
            constantPropagationArg,
            toStringArg
        )
        init()

        val performConstantPropagation = get(constantPropagationArg, false)
        val useToString = get(toStringArg, false)
    }

    protected type ConfigType = TACConfig

    protected def createConfig(args: Array[String]): TACConfig = new TACConfig(args)

    override protected def evaluate(
        cp:             Iterable[File],
        analysisConfig: TACConfig,
        execution:      Int
    ): Unit = {

        OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, ErrorLogLevel))

        val (project, _) = analysisConfig.setupProject(cp)
        if (project.projectMethodsCount == 0) {
            Console.err.println(s"Error: No methods found: $cp")
            sys.exit(-1)
        }

        if (analysisConfig.get(VerboseArg, false))
            DUVar.printDomainValue = true

        val classNames = analysisConfig.get(ClassNameArg)
        val methodSignatures = analysisConfig.get(PartialSignatureArg)

        val ch = project.classHierarchy
        for {
            cf <- project.allClassFiles
            if classNames.isEmpty || classNames.get.contains(cf.thisType.toJava)
        } {
            val methodsAsTAC = new StringBuilder()

            for {
                m <- cf.methods
                if m.body.isDefined
                mSig = (if (m.isStatic) "static " else "") + m.descriptor.toJava(m.name)
                if methodSignatures.isEmpty || methodSignatures.get.exists(sig => mSig.contains(sig._2 + sig._3))
            } {
                val (tac: String, cfg: String, ehs: Option[String]) =
                    if (analysisConfig.get(NaiveTACArg, false)) {
                        val tac @ TACode(params, code, _, cfg, ehs) =
                            TACNaive(m, ch, AllNaiveTACodeOptimizations)
                        if (analysisConfig.useToString) Console.out.println(m.toJava(tac.toString))

                        (
                            ToTxt(params, code, cfg, skipParams = true, true, true).mkString("\n"),
                            tacToDot(code, cfg),
                            if (ehs.nonEmpty)
                                Some(ehs.mkString("\n\n      /*\n      ", "\n      ", "\n      */"))
                            else
                                None
                        )
                    } else {
                        val d = analysisConfig(DomainArg).getConstructor(
                            classOf[Project[?]],
                            classOf[Method]
                        ).newInstance(project, m).asInstanceOf[Domain & RecordDefUse]

                        val aiResult = BaseAI(m, d)
                        val classHierarchy = project.classHierarchy
                        val tac @ TACode(params, code, _, cfg, ehs) =
                            TACAI(m, classHierarchy, aiResult, analysisConfig.performConstantPropagation)(Nil)
                        if (analysisConfig.useToString) Console.out.println(m.toJava(tac.toString))

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
                if (analysisConfig.get(CFGArg, false)) {
                    if (analysisConfig.get(TempFileArg, false)) {
                        Console.println(
                            "wrote cfg to: " + writeAndOpen(cfg, m.toJava, ".cfg.gv")
                        )
                    } else {
                        methodsAsTAC.append("\n/* - CFG")
                        methodsAsTAC.append(cfg)
                        methodsAsTAC.append("*/\n")
                    }
                }
                methodsAsTAC.append("\n}\n\n")
            }

            if (analysisConfig.get(TempFileArg, false)) {
                val prefix = cf.thisType.toJava
                val suffix = if (analysisConfig.get(NaiveTACArg, false)) ".naive-tac.txt" else ".ai-tac.txt"
                val targetFile = writeAndOpen(methodsAsTAC.toString(), prefix, suffix)
                Console.println("wrote tac code to: " + targetFile)
            } else {
                Console.println(methodsAsTAC.toString())
            }
        }
    }
}
