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
package fpcf
package analyses

import org.opalj.br.analyses.Project
import java.net.URL

import org.opalj.tac.DefaultTACAIKey
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info

/**
 * A small demo that shows how to use the [[org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis]]
 * and what are the results of it.
 *
 * @author Florian Kübler
 */
object SimpleEscapeAnalysisDemo extends DefaultOneStepAnalysis {

    override def title: String = "determines escape information"

    override def description: String = {
        "Determines escape information for every allocation site and every formal parameter"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val logContext = project.logContext

        // Get the TAC code for all methods to make it possible to measure the time for
        // the analysis itself.
        time {
            val tacai = project.get(DefaultTACAIKey)
            // parallelization is more efficient using parForeachMethodWithBody
            val errors = project.parForeachMethodWithBody() { mi ⇒ tacai(mi.method) }
            errors.foreach { e ⇒ error("progress", "generating 3-address code failed", e) }
        } { t ⇒ info("progress", s"generating 3-address code took ${t.toSeconds}") }

        PropertyStoreKey.makeAllocationSitesAvailable(project)
        PropertyStoreKey.makeFormalParametersAvailable(project)
        val propertyStore = project.get(PropertyStoreKey)
        time {
            SimpleEscapeAnalysis.start(project)
            propertyStore.waitOnPropertyComputationCompletion(
                resolveCycles = true,
                useFallbacksForIncomputableProperties = false
            )
        } { t ⇒ info("progress", s"escape analysis took ${t.toSeconds}") }

        /*val staticEscapes = propertyStore.entities(GlobalEscapeViaStaticFieldAssignment)
        val heapEscapes = propertyStore.entities(GlobalEscapeViaHeapObjectAssignment)
        val maybeNoEscape = propertyStore.entities(MaybeNoEscape)
        val maybeArgEscape = propertyStore.entities(MaybeArgEscape)
        val maybeMethodEscape = propertyStore.entities(MaybeMethodEscape)
        val argEscapes = propertyStore.entities(EscapeInCallee)
        val returnEscapes = propertyStore.entities(MethodEscapeViaReturn)
        val returnAssignmentEscapes = propertyStore.entities(MethodEscapeViaReturnAssignment)
        val parameterEscapes = propertyStore.entities(MethodEscapeViaParameterAssignment)
        val noEscape = propertyStore.entities(NoEscape)

        def countAS(entities: Traversable[Entity]) = entities.count(_.isInstanceOf[AllocationSite])
        def countFP(entities: Traversable[Entity]) = entities.count(_.isInstanceOf[FormalParameter])

        val message = s"""|ALLOCATION SITES:
             |# of local objects: ${countAS(noEscape)}
             |# of arg escaping objects: ${countAS(argEscapes)}
             |# of method escaping objects via return: ${countAS(returnEscapes)}
             |# of method escaping objects via return assignment: ${countAS(returnAssignmentEscapes)}
             |# of method escaping objects via parameter assignment: ${countAS(parameterEscapes)}
             |# of global escaping objects: ${countAS(staticEscapes)}
             |# of indirect global escaping objects: ${countAS(heapEscapes)}
             |# of maybe no escaping objects: ${countAS(maybeNoEscape)}
             |# of maybe arg escaping objects: ${countAS(maybeArgEscape)}
             |# of maybe method escaping objects: ${countAS(maybeMethodEscape)}
             |
             |FORMAL PARAMETERS
             |# of local objects: ${countFP(noEscape)}
             |# of arg escaping objects: ${countFP(argEscapes)}
             |# of method escaping objects via return: ${countFP(returnEscapes)}
             |# of method escaping objects via return assignment: ${countFP(returnAssignmentEscapes)}
             |# of method escaping objects via parameter assignment: ${countFP(parameterEscapes)}
             |# of global escaping objects: ${countFP(staticEscapes)}
             |# of indirect global escaping objects: ${countFP(heapEscapes)}
             |# of maybe no escaping objects: ${countFP(maybeNoEscape)}
             |# of maybe arg escaping objects: ${countFP(maybeArgEscape)}
             |# of maybe method escaping objects: ${countFP(maybeMethodEscape)}"""
*/
        BasicReport("") //message.stripMargin('|'))
    }

}
