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

import org.scalatest.FunSuite

import java.util.concurrent.atomic.AtomicInteger

import org.opalj.util.PerformanceEvaluation.timed
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.br.TestSupport.allBIProjects
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.MethodInfo
import org.opalj.br.instructions.LocalVariableAccess

/**
 * Just tests if we can compute various information for a wide range of methods; e.g.,
 * the stack depth, max locals, liveVariables.
 *
 * @author Michael Eichberg
 */
class CodePropertiesTest extends FunSuite {

    def analyzeProject(name: String, project: SomeProject): String = {
        val (t, analyzedMethodsCount) = timed { doAnalyzeProject(name, project) }
        s"$name: the analysis of $analyzedMethodsCount methods took ${t.toSeconds}"
    }

    def doAnalyzeProject(name: String, project: SomeProject): Int = {

        val ch = project.classHierarchy

        val analyzedMethodsCount = new AtomicInteger(0)
        val errors = project.parForeachMethodWithBody(() ⇒ false) { m ⇒

            val MethodInfo(src, classFile, method) = m
            val code = method.body.get
            val instructions = code.instructions
            val eh = code.exceptionHandlers
            val specifiedMaxStack = code.maxStack
            val specifiedMaxLocals = code.maxLocals

            val liveVariables = code.liveVariables(ch)
            assert(
                code.programCounters.forall(pc ⇒ liveVariables(pc) ne null),
                s"computation of liveVariables fail for ${method.toJava(classFile)}"
            )

            for { (pc, LocalVariableAccess(i, isRead)) ← code } {
                val isLive = liveVariables(pc).contains(i)
                if (isRead)
                    assert(isLive, s"$i is not live at $pc in ${method.toJava(classFile)}")
                else
                    assert(!isLive, s"$i is live at $pc in ${method.toJava(classFile)}")
            }

            val computedMaxLocals = Code.computeMaxLocalsRequiredByCode(instructions)
            if (computedMaxLocals > specifiedMaxLocals) {
                fail(
                    s"$src: computed max locals is too large - ${method.toJava(classFile)}}: "+
                        s"$specifiedMaxLocals(specified) vs. $computedMaxLocals(computed):\n"+
                        code.toString
                )
            }

            val computedMaxStack = Code.computeMaxStack(instructions, ch, eh)
            analyzedMethodsCount.incrementAndGet()
            if (specifiedMaxStack < computedMaxStack) {
                fail(
                    s"$src: computed max stack is too large - ${method.toJava(classFile)}}: "+
                        s"$specifiedMaxStack(specified) vs. $computedMaxStack(computed):\n"+
                        code.toString
                )
            }
        }
        if (errors.nonEmpty) {
            fail(errors.mkString("computation of max stack/locals failed:\n", "\n", "\n"))
        }
        analyzedMethodsCount.get()
    }

    //
    // Configuration of the tested projects
    //

    allBIProjects() foreach { biProject ⇒
        val (name, createProject) = biProject
        test(s"computation of maxStack/maxLocals for all methods of $name") {
            val count = analyzeProject(name, createProject())
            info(s"computation of maxStack/maxLocals succeeded for $count methods")
        }
    }

    test(s"computation of maxStack/maxLocals for all methods of the JDK ($JRELibraryFolder)") {
        analyzeProject("JDK", TestSupport.createJREProject)
    }

}
