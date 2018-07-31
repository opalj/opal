/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
            EagerInterProceduralEscapeAnalysis.start(project, null)
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
