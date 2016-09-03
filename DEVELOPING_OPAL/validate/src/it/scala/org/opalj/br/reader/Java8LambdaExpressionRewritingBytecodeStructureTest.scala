/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package br
package reader

import analyses.{Project, SomeProject}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.opalj.br.ClassFile
import org.opalj.br.ClassValue
import org.opalj.br.ElementValuePair
import org.opalj.br.Method
import org.opalj.br.MethodWithBody
import org.opalj.br.StringValue
import org.opalj.bi.TestSupport
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.ai.domain.l0.BaseDomain
import org.opalj.ai.BaseAI
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.opalj.ai.InterpretationFailedException

/**
 * Test that code with rewritten invokedynamic instructions is still valid bytecode.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class Java8LambdaExpressionRewritingBytecodeStructureTest extends FunSpec with Matchers {

    def verifyMethod(testProject: SomeProject, classFile: ClassFile, method: Method): Unit = {
        assert(method.body.get.instructions.nonEmpty, s"empty method: ${method.toJava(classFile)}")

        val domain = new BaseDomain(testProject, classFile, method)
        try {
            val result = BaseAI(classFile, method, domain)
            // the abstract interpretation succeed
            result should not be ('wasAborted)
            val instructions = method.body.get.instructions
            // the layout of the instructions array is correct
            for { pc ← 0 until instructions.size } {
                if (instructions(pc) != null) {
                    val nextPc = instructions(pc).indexOfNextInstruction(pc, false)
                    instructions.slice(pc + 1, nextPc).foreach(_ should be(null))
                }
            }
        } catch {
            case e: InterpretationFailedException ⇒ {
                val msg = e.getMessage+"\n"+
                    (if (e.getCause != null) "\tcause: "+e.getCause.getMessage+"\n" else "") +
                    s"\tAt PC ${e.pc}\n"+
                    s"\twith stack:\n${e.operandsArray(e.pc).mkString(", ")}\n"+
                    method.toJava(classFile) + method.body.get.instructions.zipWithIndex.mkString("\n\t\t", "\n\t\t", "\n")
                fail(msg)
            }
        }
    }

    def testProject(project: SomeProject): Unit = {
        for {
            classFile ← project.allProjectClassFiles.par
            method @ MethodWithBody(body) ← classFile.methods
            instructions = body.instructions
            if instructions.exists {
                case i: INVOKESTATIC ⇒ i.declaringClass.fqn.matches("^Lambda\\$\\d+:\\d+$")
            }
        } {
            verifyMethod(project, classFile, method)
        }
    }

    val testProjectFile = TestSupport.locateTestResources("classfiles/Lambdas.jar", "br")

    describe("Testing a test project and the JRE with and without caching") {
        describe("Testing the test project without caching") {
            val project: SomeProject = Project(
                Java8FrameworkWithLambdaSupport.ClassFiles(testProjectFile),
                Java8LibraryFramework.ClassFiles(util.JRELibraryFolder)
            )
            testProject(project)
        }

        describe("Testing the test project with caching") {
            val cache = new BytecodeInstructionsCache()
            val framework = new Java8FrameworkWithLambdaSupportAndCaching(cache)
            val project: SomeProject = Project(
                framework.ClassFiles(testProjectFile),
                Java8LibraryFramework.ClassFiles(util.JRELibraryFolder)
            )
            testProject(project)
        }

        describe("Testing the JRE rt.jar without caching") {
            val project: SomeProject = Project(
                Java8FrameworkWithLambdaSupport.ClassFiles(util.RTJar)
            )
            testProject(project)
        }

        describe("Testing the JRE rt.jar with caching") {
            val cache = new BytecodeInstructionsCache()
            val framework = new Java8FrameworkWithLambdaSupportAndCaching(cache)
            val project: SomeProject = Project(
                framework.ClassFiles(util.RTJar)
            )
            testProject(project)
        }
    }
}
