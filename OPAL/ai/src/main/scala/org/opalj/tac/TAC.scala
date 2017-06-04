/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package tac

import org.opalj.io.writeAndOpen
import org.opalj.io.OpeningFileFailedException
import org.opalj.graphs.toDot
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.reader.Java8Framework
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.br.cfg.CFGFactory

/**
 * Creates the three-address representation and prints it.
 *
 * @example
 *         To convert all files of a project to TAC you can use:
 * {{{
 * import org.opalj.io.write
 * import org.opalj.tac._
 * import org.opalj.util.PerformanceEvaluation.time
 * val f = new java.io.File("/Users/eichberg/Downloads/presto-verifier-0.147-executable.zip")
 * val p = org.opalj.br.analyses.Project(f)
 * var i = 0
 * time {
 * p.parForeachMethodWithBody(parallelizationLevel=32){ mi =>
 *   val (code,_) = org.opalj.tac.AsQuadruples(mi.method,p.classHierarchy)
 *   val tac = ToJavaLike(code)
 *   val fileNamePrefix = mi.classFile.thisType.toJava+"."+mi.method.name
 *   val file = write(tac, fileNamePrefix, ".tac.txt")
 *   i+= 1
 *   println(i+":"+file)
 * }
 * }(t => println("Analysis time: "+t.toSeconds))
 * }}}
 *
 * @author Michael Eichberg
 */
object TAC {

    private final val Usage = {
        "Usage: java …TAC \n"+
            "(1) <JAR file containing class files>\n"+
            "(2) <class file name>\n"+
            "(3) <method name>\n"+
            "[(4) ai|naive|both (default: ai)]"+
            "Example:\n\tjava …TAC /Library/jre/lib/rt.jar java.util.ArrayList toString"
    }

    def processMethod(
        project:   SomeProject,
        classFile: ClassFile,
        method:    Method,
        use:       String
    ): Unit = {
        val naiveCFGFile = writeAndOpen(
            CFGFactory(method.body.get, project.classHierarchy).toDot,
            "NaiveCFG-"+method.name, ".br.cfg.gv"
        )
        println(s"Generated naive CFG (for comparison purposes only) $naiveCFGFile.")

        try {
            val ch = project.classHierarchy

            if (use == "ai" || use == "both") { // USING AI
                val domain = new DefaultDomainWithCFGAndDefUse(project, classFile, method)
                val aiResult = BaseAI(classFile, method, domain)
                val aiCFGFile = writeAndOpen(
                    toDot(Set(aiResult.domain.cfgAsGraph())),
                    "AICFG-"+method.name, ".ai.cfg.gv"
                )
                println(s"Generated ai CFG (input) $aiCFGFile.")
                val aiBRCFGFile = writeAndOpen(aiResult.domain.bbCFG.toDot, "AICFG", "ai.br.cfg.gv")
                println(s"Generated the reified ai CFG $aiBRCFGFile.")
                val (code, cfg) = TACAI(method, project.classHierarchy, aiResult)(List.empty)
                val graph = tacToDot(code, cfg)
                val tacCFGFile = writeAndOpen(graph, "TACCFG-"+method.name, ".tac.cfg.gv")
                println(s"Generated the tac cfg file $tacCFGFile.")
                val tac = ToTxt(code)
                val fileNamePrefix = classFile.thisType.toJava+"."+method.name
                val file = writeAndOpen(tac, fileNamePrefix, ".ai.tac.txt")
                println(s"Generated the ai tac file $file.")
            }

            if (use == "naive" || use == "both") { // USING NO AI
                val (code, _) =
                    TACNaive(method, ch, AllTACNaiveOptimizations, forceCFGCreation = true)

                val tac = ToTxt(code)
                val fileNamePrefix = classFile.thisType.toJava+"."+method.name
                val file = writeAndOpen(tac, fileNamePrefix, ".naive.tac.txt")
                println(s"Generated the naive tac file $file.")
            }

        } catch {
            case OpeningFileFailedException(file, cause) ⇒
                println(s"Opening the tac file $file failed: ${cause.getMessage()}")
        }
    }

    def main(args: Array[String]): Unit = {

        if (args.length < 3 || args.length > 4) {
            println(Usage)
            sys.exit(-1)
        }
        /*
        println("Sleeping for five seconds"); Thread.sleep(5000)
        */

        val jarName = args(0)
        val classFiles = Java8Framework.ClassFiles(new java.io.File(jarName))
        val project = Project(classFiles)
        if (classFiles.isEmpty) {
            println(s"No classfiles found in ${args(0)}")
        } else {
            val clazzName = args(1)
            val methodName = args(2)
            val useAI = if (args.length == 4) args(3).toLowerCase else "both"

            val classFile = classFiles.find(e ⇒ e._1.thisType.toJava == clazzName).map(_._1).get
            val methods = classFile.findMethod(methodName)
            if (methods.isEmpty) {
                val methodNames = classFile.methods.map(_.name)
                val messageHead = s"cannot find the method: $methodName (Available: "
                println(methodNames.mkString(messageHead, ",", ")"))
            } else {
                methods.foreach { method ⇒ processMethod(project, classFile, method, useAI) }
            }
        }
    }
}
