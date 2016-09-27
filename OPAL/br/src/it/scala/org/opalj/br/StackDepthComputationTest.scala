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
package cfg

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import java.io.File
import java.io.FilenameFilter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.JavaConverters._

import org.opalj.util.PerformanceEvaluation._
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java9FrameworkWithLambdaExpressionsSupportAndCaching
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.MethodInfo

/**
 * Just tests if we can compute the stack depth for a wide range of methods.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class StackDepthComputationTest extends FunSpec with Matchers {

    def analyzeProject(name: String, project: SomeProject): Unit = {
        var analyzedMethodsCount: Int = 0
        time {
            analyzedMethodsCount = doAnalyzeProject(name, project)
        } { t ⇒ info(s"the analysis of $analyzedMethodsCount methods took ${t.toSeconds}") }
    }

    def doAnalyzeProject(name: String, project: SomeProject): Int = {

        val classHierarchy = project.classHierarchy

        val analyzedMethodsCount = new AtomicInteger(0)
        val errors = new ConcurrentLinkedQueue[String]

        project.parForeachMethodWithBody(() ⇒ false) { m ⇒

            import Code.computeMaxStack

            val MethodInfo(_, classFile, method) = m
            val code = method.body.get
            val instructions = code.instructions
            val exceptionHandlers = code.exceptionHandlers
            val specifiedMaxStack = code.maxStack
try {
            val computedMaxStack = computeMaxStack(instructions, classHierarchy, exceptionHandlers)
            analyzedMethodsCount.incrementAndGet()
            if (specifiedMaxStack < computedMaxStack) {
                errors.add(
                    s"the computed max stack is too large for ${method.toJava(classFile)}}: "+
                        s"$specifiedMaxStack(specified) vs. $computedMaxStack(computed)"
                )
            }
} catch {
    case t : Throwable => errors.add(t.getMessage)
}
        }
        if (!errors.isEmpty()) {
            fail(errors.asScala.mkString("computation of max stack failed:\n\t", "\n\t", "\n"))
        }
        analyzedMethodsCount.get()
    }

    //
    // Configuration of the tested projects
    //

    describe("computing the stack depth (Code.max_stack)") {

        val cache = new BytecodeInstructionsCache
        val reader = new Java9FrameworkWithLambdaExpressionsSupportAndCaching(cache)
        import reader.AllClassFiles

        it(s"should be possible for all methods of the JDK ($JRELibraryFolder)") {
            val project = createJREProject
            analyzeProject("JDK", project)
        }

        val classFilesFolder = locateTestResources("classfiles", "bi")
        val filter = new FilenameFilter() {
            def accept(dir: File, name: String) = { name.endsWith(".jar") }
        }
        val jars = classFilesFolder.listFiles(filter)
        jars.foreach { jar ⇒
            it(s"should be possible for all methods of $jar") {
                val project = Project(AllClassFiles(Seq(jar)), Traversable.empty, true)
                analyzeProject(jar.getName, project)
            }
        }
    }
}
