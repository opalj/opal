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

import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.br.AllocationSite
import org.opalj.br.Method
import org.opalj.tac.DefaultTACAIKey
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.FormalParameter
import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis
import org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.EscapeViaParameterAndReturn
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info

/**
 * A small demo that shows how to use the
 * [[org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis]] and what are the results of it.
 *
 * @author Florian Kübler
 */
object InterProceduralEscapeAnalysisDemo extends DefaultOneStepAnalysis {

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

        val propertyStore = time {
            project.getOrCreateProjectInformationKeyInitializationData(
                SimpleAIKey,
                (m: Method) ⇒ {
                    new DefaultDomainWithCFGAndDefUse(project, m) //new DefaultPerformInvocationsDomainWithCFGAndDefUse(project, m) // with org.opalj.ai.domain.l1.DefaultArrayValuesBinding
                }
            )
            PropertyStoreKey.makeAllocationSitesAvailable(project)
            PropertyStoreKey.makeFormalParametersAvailable(project)
            PropertyStoreKey.makeVirtualFormalParametersAvailable(project)
            project.get(PropertyStoreKey)
        } { t ⇒ info("progress", s"initialization of property store took ${t.toSeconds}") }

        // Get the TAC code for all methods to make it possible to measure the time for
        // the analysis itself.
        time {
            val tacai = project.get(DefaultTACAIKey)
            // parallelization is more efficient using parForeachMethodWithBody
            val errors = project.parForeachMethodWithBody() { mi ⇒ tacai(mi.method) }
            errors.foreach { e ⇒ error("progress", "generating 3-address code failed", e) }
        } { t ⇒ info("progress", s"generating 3-address code took ${t.toSeconds}") }

        time {
            InterProceduralEscapeAnalysis.start(project)
            propertyStore.waitOnPropertyComputationCompletion(useFallbacksForIncomputableProperties = false)
        } { t ⇒ info("progress", s"escape analysis took ${t.toSeconds}") }

        def countAS(entities: Traversable[Entity]) = entities.count(_.isInstanceOf[AllocationSite])
        def countFP(entities: Traversable[Entity]) = entities.count(_.isInstanceOf[FormalParameter])

        val message =
            s"""|ALLOCATION SITES:
                |# of local objects: ${countAS(propertyStore.entities(NoEscape))}
                |# of objects escaping in a callee: ${countAS(propertyStore.entities(EscapeInCallee))}
                |# of escaping objects via return: ${countAS(propertyStore.entities(EscapeViaReturn))}
                |# of escaping objects via abnormal return: ${countAS(propertyStore.entities(EscapeViaAbnormalReturn))}
                |# of escaping objects via parameter: ${countAS(propertyStore.entities(EscapeViaParameter))}
                |# of escaping objects via normal and abnormal return: ${countAS(propertyStore.entities(EscapeViaNormalAndAbnormalReturn))}
                |# of escaping objects via parameter and normal return: ${countAS(propertyStore.entities(EscapeViaParameterAndReturn))}
                |# of escaping objects via parameter and abnormal return: ${countAS(propertyStore.entities(EscapeViaParameterAndAbnormalReturn))}
                |# of escaping objects via parameter and normal and abnormal return: ${countAS(propertyStore.entities(EscapeViaParameterAndNormalAndAbnormalReturn))}
                |# of escaping objects via static field: ${countAS(propertyStore.entities(EscapeViaStaticField))}
                |# of escaping objects via heap objects: ${countAS(propertyStore.entities(EscapeViaHeapObject))}
                |# of global escaping objects: ${countAS(propertyStore.entities(GlobalEscape))}
                |
                |FORMAL PARAMETERS:
                |# of local objects: ${countFP(propertyStore.entities(NoEscape))}
                |# of objects escaping in a callee: ${countFP(propertyStore.entities(EscapeInCallee))}
                |# of escaping objects via return: ${countFP(propertyStore.entities(EscapeViaReturn))}
                |# of escaping objects via abnormal return: ${countFP(propertyStore.entities(EscapeViaAbnormalReturn))}
                |# of escaping objects via parameter: ${countFP(propertyStore.entities(EscapeViaParameter))}
                |# of escaping objects via normal and abnormal return: ${countFP(propertyStore.entities(EscapeViaNormalAndAbnormalReturn))}
                |# of escaping objects via parameter and normal return: ${countFP(propertyStore.entities(EscapeViaParameterAndReturn))}
                |# of escaping objects via parameter and abnormal return: ${countFP(propertyStore.entities(EscapeViaParameterAndAbnormalReturn))}
                |# of escaping objects via parameter and normal and abnormal return: ${countFP(propertyStore.entities(EscapeViaParameterAndNormalAndAbnormalReturn))}
                |# of escaping objects via static field: ${countFP(propertyStore.entities(EscapeViaStaticField))}
                |# of escaping objects via heap objects: ${countFP(propertyStore.entities(EscapeViaHeapObject))}
                |# of global escaping objects: ${countFP(propertyStore.entities(GlobalEscape))}"""

        BasicReport(message.stripMargin('|'))
    }

}
