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

import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.br.Method
import org.opalj.tac.DefaultTACAIKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.log.LogContext
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.log.OPALLogger.info
import org.opalj.tac.DVar
import org.opalj.tac.New
import org.opalj.tac.Assignment
import org.opalj.tac.NewArray
import org.opalj.tac.MonitorEnter

/**
 * Finds object references in monitorenter instructions that do not escape their thread.
 *
 * @author Florian Kübler
 */
object UnnecessarySynchronizationAnalysis extends DefaultOneStepAnalysis {

    override def title: String = "Finds unnecessary usage of synchronization"

    override def description: String = {
        "Finds unnecessary usage of synchronization"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val logContext: LogContext = project.logContext

        val propertyStore = time {

            val domain = (m: Method) ⇒ new DefaultPerformInvocationsDomainWithCFGAndDefUse(project, m)
            project.getOrCreateProjectInformationKeyInitializationData(SimpleAIKey, domain)

            project.get(PropertyStoreKey)
        } { t ⇒ info("progress", s"initialization of property store took ${t.toSeconds}") }

        val tacai = time {
            val tacai = project.get(DefaultTACAIKey)
            tacai
        } { t ⇒ info("progress", s"generating 3-address code took ${t.toSeconds}") }

        time {
            EagerInterProceduralEscapeAnalysis.start(project)
            propertyStore.waitOnPhaseCompletion()
        } { t ⇒ info("progress", s"escape analysis took ${t.toSeconds}") }

        val allocationSites = project.get(DefinitionSitesKey).getAllocationSites
        val objects = time {
            for {
                as ← allocationSites
                method = as.method
                FinalEP(_, escape) = propertyStore(as, EscapeProperty.key)
                if EscapeViaNormalAndAbnormalReturn lessOrEqualRestrictive escape
                code = tacai(method).stmts
                defSite = code indexWhere (stmt ⇒ stmt.pc == as.pc)
                if defSite != -1
                stmt = code(defSite)
                if stmt.astID == Assignment.ASTID
                Assignment(_, DVar(_, uses), New(_, _) | NewArray(_, _, _)) = code(defSite)
                if uses exists { use ⇒
                    code(use) match {
                        case MonitorEnter(_, v) if v.asVar.definedBy.contains(defSite) ⇒ true
                        case _ ⇒ false
                    }
                }
            } yield as
        } { t ⇒ info("progress", s"unnecessary synchronization analysis took ${t.toSeconds}") }

        val message =
            s"""|Objects that were unnecessarily synchronized:
                |${objects.mkString("\n|")}
             """

        BasicReport(message.stripMargin('|'))
    }

}
