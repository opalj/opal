/* BSD 2-Clause License:
 * Copyright (c) 2009 - 201&
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
package org.opalj.br

import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.atomic.AtomicInteger
import java.lang.{Boolean => JBoolean}
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.opalj.util.PerformanceEvaluation.timed
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.MethodInfo
import org.opalj.br.analyses.Project
import org.opalj.br.cfg.CFGFactory
import org.opalj.br.instructions.LocalVariableAccess
import org.opalj.br.reader.Java9Framework
import org.opalj.br.reader.BytecodeOptimizer
import org.opalj.collection.immutable.IntArraySet
import org.opalj.concurrent.ConcurrentExceptions

/**
 * Just tests if we can compute various information for a wide range of methods; e.g.,
 * the stack depth, max locals, liveVariables.
 *
 * @author Michael Eichberg
 */
class CodePropertiesTest extends AnyFunSuite {

    def analyzeMaxStackAndLocals(project: SomeProject): String = {
        try {
            val (t, analyzedMethodsCount) = timed { doAnalyzeMaxStackAndLocals(project) }
            s"computing max stack/locals for all $analyzedMethodsCount methods took ${t.toSeconds}"
        } catch {
            case ce: ConcurrentExceptions =>
                ce.getSuppressed.foreach(_.printStackTrace(Console.err))
                throw ce
        }
    }

    def doAnalyzeMaxStackAndLocals(project: SomeProject): Int = {

        val ch = project.classHierarchy
        val TyperTyperType = ObjectType("scala/tools/nsc/typechecker/Typers$Typer")

        val analyzedMethodsCount = new AtomicInteger(0)
        project.parForeachMethodWithBody() { m =>

            val MethodInfo(src, method) = m
            val declaringClassType = method.declaringClassFile.thisType
            val code = method.body.get
            val instructions = code.instructions
            val eh = code.exceptionHandlers
            val specifiedMaxStack = code.maxStack
            val specifiedMaxLocals = code.maxLocals
            val cfg = CFGFactory(code, ch)

            val liveVariables = code.liveVariables(ch)
            assert(
                code.programCounters.forall(pc => liveVariables(pc) ne null),
                s"computation of liveVariables fails for ${method.toJava}"
            )

            for {
                PCAndInstruction(pc, instruction) <- code
                if instruction.isReturnInstruction
                // The bytecode of the scala...typechecker.Typers$Typer.$deserializeLambda$ method
                // is invalid. The "primary" code is duplicated in an exception handler and the
                // stack at the ARETURN in case of the exception is therefore not 0.
                // This causes this test to fail, ignore this method therefore.
                if method.name != "$deserializeLambda$" || declaringClassType != TyperTyperType
            } {
                val stackDepthAt = code.stackDepthAt(pc, cfg)
                val stackSlotChange = instruction.stackSlotsChange
                if (stackDepthAt + stackSlotChange != 0) {
                    val message =
                        code.instructions.zipWithIndex.map(_.swap).mkString(
                            s"stack depth at pc:$pc[$instruction]($stackDepthAt) + stack slot change($stackSlotChange) is not 0:\n\t",
                            "\n\t",
                            "\n"
                        )

                    fail(method.toJava(message))
                }
            }

            for { PCAndInstruction(pc, LocalVariableAccess(i, isRead)) <- code } {
                val isLive = liveVariables(pc).contains(i)
                if (isRead)
                    assert(isLive, s"$i is not live at $pc in ${method.toJava}")
                else
                    assert(!isLive, s"$i is live at $pc in ${method.toJava}")
            }

            val computedMaxLocals = Code.computeMaxLocalsRequiredByCode(instructions)
            if (computedMaxLocals > specifiedMaxLocals) {
                fail(
                    s"$src: computed max locals is too large - ${method.toJava}}: "+
                        s"$specifiedMaxLocals(specified) vs. $computedMaxLocals(computed):\n"+
                        code.toString
                )
            }

            val computedMaxStack = Code.computeMaxStack(instructions, eh, cfg)
            if (specifiedMaxStack < computedMaxStack) {
                fail(
                    s"$src: computed max stack is too large - ${method.toJava}}: "+
                        s"$specifiedMaxStack(specified) vs. $computedMaxStack(computed):\n"+
                        code.toString
                )
            }
            analyzedMethodsCount.incrementAndGet()
        }
        analyzedMethodsCount.get()
    }

    def analyzeStackMapTablePCs(project: SomeProject): String = {
        val (t, analyzedMethodsCount) = timed { doAnalyzeStackMapTablePCs(project) }
        s"successfully computing stack map table pcs for $analyzedMethodsCount methods took ${t.toSeconds}"
    }

    def doAnalyzeStackMapTablePCs(project: SomeProject): Int = {
        implicit val ch = project.classHierarchy
        val analyzedMethodsCount = new AtomicInteger(0)
        project.parForeachMethodWithBody(() => false) { mi =>
            if (mi.classFile.version.major > 49) {
                analyzedMethodsCount.incrementAndGet()
                val code = mi.method.body.get
                val definedPCs = code.stackMapTable.map(_.pcs).getOrElse(IntArraySet.empty)
                def validateComputedPCs(computedPCs: IntArraySet): Unit = {
                    if (computedPCs != definedPCs) {
                        if (computedPCs.size >= definedPCs.size) {
                            fail(
                                s"${mi.source}:${mi.method.toJava}: "+
                                    "computed stack map table pcs differ:\n"+
                                    definedPCs.mkString("expected:  {", ",", "}\n") +
                                    computedPCs.mkString("computed:  {", ",", "}\n")
                            )
                        } else {
                            // ... in this case, we have a strict subset; however, some compilers
                            // seem to create stack map frames which are not strictly required;
                            // we have to ignore those...
                        }
                    }
                }
                validateComputedPCs(code.stackMapTablePCs)
            }
        }
        analyzedMethodsCount.get
    }

    //
    // Configuration of the tested projects
    //

    allBIProjects() foreach { biProject =>
        val (name, createProject) = biProject
        test(s"computation of maxStack/maxLocals for all methods of $name") {
            val count = analyzeMaxStackAndLocals(createProject())
            info(s"computation of maxStack/maxLocals succeeded for $count methods")
        }
    }

    test(s"computing maxStack, maxLocals and stackMapTablePCs for $JRELibraryFolder") {

        // To make the comparison more meaningful we have to turn off bytecode optimizations;
        // (Due to the optimizations we get a new smaller cfg; however the old stack map table
        // remains valid; it just contains superfluous entries.)
        val optimizationConfigKey = BytecodeOptimizer.SimplifyControlFlowKey
        val logOptimizationsConfigKey = BytecodeOptimizer.LogControlFlowSimplificationKey
        val theConfig =
            BaseConfig
                .withValue(logOptimizationsConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.TRUE))
                .withValue(optimizationConfigKey, ConfigValueFactory.fromAnyRef(JBoolean.FALSE))
        class Reader extends Java9Framework {
            override def defaultConfig: Config = theConfig
            override def loadsInterfacesOnly: Boolean = false
        }
        val reader = new Reader()
        val cfs = org.opalj.br.reader.readJREClassFiles()(reader = reader)
        val jreProject = Project(cfs)
        analyzeMaxStackAndLocals(jreProject)
        val count = analyzeStackMapTablePCs(jreProject)
        info(s"computation of stack maps table pcs executed for $count methods")
    }

}
