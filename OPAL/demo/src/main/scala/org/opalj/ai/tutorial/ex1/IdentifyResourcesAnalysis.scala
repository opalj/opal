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
package org.opalj.ai.tutorial.ex1

import java.net.URL
import org.opalj._
import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.instructions._
import org.opalj.ai._

/**
 * @author Michael Eichberg
 */
object IdentifyResourcesAnalysis extends AnalysisExecutor {

    class AnalysisDomain(
        override val project: Project[URL],
        val method: Method)
            extends Domain
            with domain.DefaultDomainValueBinding
            with domain.ThrowAllPotentialExceptionsConfiguration
            with domain.l0.DefaultPrimitiveTypeConversions
            with domain.l0.DefaultTypeLevelIntegerValues
            with domain.l0.DefaultTypeLevelLongValues
            with domain.l0.DefaultTypeLevelFloatValues
            with domain.l0.DefaultTypeLevelDoubleValues
            with domain.l0.TypeLevelFieldAccessInstructions
            with domain.l0.TypeLevelInvokeInstructions
            // NOT NEEDED: with domain.l1.DefaultReferenceValuesBinding
            with domain.l1.DefaultStringValuesBinding
            with domain.DefaultHandlingOfMethodResults
            with domain.IgnoreSynchronization
            with domain.TheProject[URL]
            with domain.TheMethod
            with domain.ProjectBasedClassHierarchy

    val analysis = new Analysis[URL, BasicReport] {

        override def title: String = "Identifies Resources"

        override def description: String =
            "Identifies java.io.File object instantiations using constant strings."

        override def analyze(theProject: Project[URL], parameters: Seq[String]) = {
            import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

            // Step 1
            // Find all methods that create "java.io.File(<String>)" objects.
            val callSites = time {
                (for {
                    cf ← theProject.classFiles.par
                    m @ MethodWithBody(body) ← cf.methods
                } yield {
                    val pcs = for {
                        pc ← body.collectWithIndex {
                            case (
                                pc,
                                INVOKESPECIAL(
                                    ObjectType("java/io/File"),
                                    "<init>",
                                    SingleArgumentMethodDescriptor((ObjectType.String, VoidType)))
                                ) ⇒ pc
                        }
                    } yield pc
                    (cf, m, pcs)
                }).filter(_._3.size > 0)
            } { t ⇒ println(f"Finding candidates took: ${ns2sec(t)}%2.2f seconds.") }

            // Step 2
            // Perform a simple abstract interpretation to check if there is some
            // method that pass a constant string to a method
            val callSitesWithConstantStringParameter = time {
                for {
                    (cf, m, pcs) ← callSites
                    result = BaseAI(cf, m, new AnalysisDomain(theProject, m))
                    (pc, value) ← pcs.map(pc ⇒ (pc, result.operandsArray(pc))).collect {
                        case (pc, result.domain.StringValue(value) :: _) ⇒ (pc, value)
                    }
                } yield (cf, m, pc, value)
            } { t ⇒ println(f"Performing the abstract interpretations took: ${ns2sec(t)}%2.2f seconds.") }

            def callSiteToString(callSite: (ClassFile, Method, PC, String)): String = {
                val (cf, m, pc, v) = callSite
                cf.thisType.toJava+
                    "{ "+m.toJava+
                    "{"+pc+": \""+v+"\" } }"
            }

            BasicReport(
                if (callSitesWithConstantStringParameter.isEmpty)
                    "Only found "+callSites.size+" candidates."
                else
                    callSitesWithConstantStringParameter.map(callSiteToString(_)).
                        mkString("Methods:\n", "\n", ".\n"))
        }
    }
}



