/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.PropertyStoreKey
import java.net.URL
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.Warn
import org.opalj.fpcf.properties.IsClientCallable
import org.opalj.fpcf.properties.NotClientCallable

/**
 * @author Michael Reif
 */
object LibraryLeakageAnalysisDemo extends MethodAnalysisDemo {

    override def title: String = "method leakage analysis"

    override def description: String = {
        "determines if the method is exposed to the client via subclasses"
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Warn))

        val propertyStore = project.get(PropertyStoreKey)
        val executer = project.get(FPCFAnalysesManagerKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {
            executer.run(CallableFromClassesInOtherPackagesAnalysis)
        } { t ⇒ analysisTime = t.toSeconds }

        val leakedMethods = propertyStore.entities { (p: Property) ⇒ p == IsClientCallable }

        val notLeakedMethods = propertyStore.entities { (p: Property) ⇒ p == NotClientCallable }
        BasicReport(
            //            nonOverriddenInfoString +
            propertyStore.toString+
                "\nAnalysis time: "+analysisTime +
                s"\nleaked: ${leakedMethods.size}"+
                s"\n not leaked: ${notLeakedMethods.size}"
        )
    }
}
