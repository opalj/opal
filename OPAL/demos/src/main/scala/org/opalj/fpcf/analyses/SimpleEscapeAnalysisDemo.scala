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

import java.net.URL

import org.opalj.ai.common.DefinitionSite
import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.fpcf.analyses.escape.EagerSimpleEscapeAnalysis
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndReturn
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.tac.DefaultTACAIKey
import org.opalj.util.PerformanceEvaluation.time

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
        implicit val logContext: LogContext = project.logContext

        val propertyStore = time {

            project.getOrCreateProjectInformationKeyInitializationData(
                SimpleAIKey,
                (m: Method) ⇒ {
                    new PrimitiveTACAIDomain(project, m)
                }
            )
            project.get(PropertyStoreKey)
        } { t ⇒ info("progress", s"initialization of property store took ${t.toSeconds}") }
        PropertyStore.updateDebug(true)

        // Get the TAC code for all methods to make it possible to measure the time for
        // the analysis itself.
        time {
            project.get(DefaultTACAIKey)
            // parallelization is more efficient using parForeachMethodWithBody
        } { t ⇒ info("progress", s"generating 3-address code took ${t.toSeconds}") }

        time {
            propertyStore.setupPhase(Set(EscapeProperty.key))
            EagerSimpleEscapeAnalysis.start(project)
            propertyStore.waitOnPhaseCompletion()
        } { t ⇒ info("progress", s"escape analysis took ${t.toSeconds}") }

        def countAS(entities: Iterator[Entity]) = entities.count(_.isInstanceOf[DefinitionSite])

        // we are only interested in the this locals of the constructors
        def countFP(entities: Iterator[Entity]) = entities.collect { case e @ VirtualFormalParameter(DefinedMethod(_, m), -1) if m.isConstructor ⇒ e }.size

        val message =
            s"""|ALLOCATION SITES:
                |# of local objects: ${countAS(propertyStore.finalEntities(NoEscape))}
                |# of objects escaping in a callee: ${countAS(propertyStore.finalEntities(EscapeInCallee))}
                |# of escaping objects via return: ${countAS(propertyStore.finalEntities(EscapeViaReturn))}
                |# of escaping objects via abnormal return: ${countAS(propertyStore.finalEntities(EscapeViaAbnormalReturn))}
                |# of escaping objects via parameter: ${countAS(propertyStore.finalEntities(EscapeViaParameter))}
                |# of escaping objects via normal and abnormal return: ${countAS(propertyStore.finalEntities(EscapeViaNormalAndAbnormalReturn))}
                |# of escaping objects via parameter and normal return: ${countAS(propertyStore.finalEntities(EscapeViaParameterAndReturn))}
                |# of escaping objects via parameter and abnormal return: ${countAS(propertyStore.finalEntities(EscapeViaParameterAndAbnormalReturn))}
                |# of escaping objects via parameter and normal and abnormal return: ${countAS(propertyStore.finalEntities(EscapeViaParameterAndNormalAndAbnormalReturn))}
                |# of escaping objects via static field: ${countAS(propertyStore.finalEntities(EscapeViaStaticField))}
                |# of escaping objects via heap objects: ${countAS(propertyStore.finalEntities(EscapeViaHeapObject))}
                |# of global escaping objects: ${countAS(propertyStore.finalEntities(GlobalEscape))}
                |# of at most local object: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(NoEscape)) ⇒ e })}
                |# of escaping object at most in callee: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeInCallee)) ⇒ e })}
                |# of escaping object at most via return: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaReturn)) ⇒ e })}
                |# of escaping object at most via abnormal return: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaAbnormalReturn)) ⇒ e })}
                |# of escaping object at most via parameter: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaParameter)) ⇒ e })}
                |# of escaping object at most via normal and abnormal return: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaNormalAndAbnormalReturn)) ⇒ e })}
                |# of escaping object at most via parameter and normal return: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaParameterAndReturn)) ⇒ e })}
                |# of escaping object at most via parameter and abnormal return: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaParameterAndAbnormalReturn)) ⇒ e })}
                |# of escaping object at most via parameter and normal and abnormal return: ${countAS(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)) ⇒ e })}
                |
                |
                |FORMAL PARAMETERS:
                |# of local objects: ${countFP(propertyStore.finalEntities(NoEscape))}
                |# of objects escaping in a callee: ${countFP(propertyStore.finalEntities(EscapeInCallee))}
                |# of escaping objects via return: ${countFP(propertyStore.finalEntities(EscapeViaReturn))}
                |# of escaping objects via abnormal return: ${countFP(propertyStore.finalEntities(EscapeViaAbnormalReturn))}
                |# of escaping objects via parameter: ${countFP(propertyStore.finalEntities(EscapeViaParameter))}
                |# of escaping objects via normal and abnormal return: ${countFP(propertyStore.finalEntities(EscapeViaNormalAndAbnormalReturn))}
                |# of escaping objects via parameter and normal return: ${countFP(propertyStore.finalEntities(EscapeViaParameterAndReturn))}
                |# of escaping objects via parameter and abnormal return: ${countFP(propertyStore.finalEntities(EscapeViaParameterAndAbnormalReturn))}
                |# of escaping objects via parameter and normal and abnormal return: ${countFP(propertyStore.finalEntities(EscapeViaParameterAndNormalAndAbnormalReturn))}
                |# of escaping objects via static field: ${countFP(propertyStore.finalEntities(EscapeViaStaticField))}
                |# of escaping objects via heap objects: ${countFP(propertyStore.finalEntities(EscapeViaHeapObject))}
                |# of global escaping objects: ${countFP(propertyStore.finalEntities(GlobalEscape))}
                |# of at most local object: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(NoEscape)) ⇒ e })}
                |# of escaping object at most in callee: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeInCallee)) ⇒ e })}
                |# of escaping object at most via return: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaReturn)) ⇒ e })}
                |# of escaping object at most via abnormal return: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaAbnormalReturn)) ⇒ e })}
                |# of escaping object at most via parameter: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaParameter)) ⇒ e })}
                |# of escaping object at most via normal and abnormal return: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaNormalAndAbnormalReturn)) ⇒ e })}
                |# of escaping object at most via parameter and normal return: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaParameterAndReturn)) ⇒ e })}
                |# of escaping object at most via parameter and abnormal return: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaParameterAndAbnormalReturn)) ⇒ e })}
                |# of escaping object at most via parameter and normal and abnormal return: ${countFP(propertyStore.entities(EscapeProperty.key).collect { case FinalEP(e, AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)) ⇒ e })}"""

        BasicReport(message.stripMargin('|'))
    }

}
