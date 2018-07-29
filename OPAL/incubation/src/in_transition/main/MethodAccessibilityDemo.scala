/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.analyses.BasicReport

import java.net.URL
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.log.ConsoleOPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.fpcf.properties.Global
import org.opalj.fpcf.properties.PackageLocal
import org.opalj.fpcf.properties.ClassLocal

/**
 * @author Michael Reif
 */
object MethodAccessibilityAnalysisDemo extends MethodAnalysisDemo {

    OPALLogger.updateLogger(GlobalLogContext, new ConsoleOPALLogger(true, Warn))

    override def title: String =
        "entry point computation"

    override def description: String =
        "determines the factory methods of a library"

    override def doAnalyze(
        project:       org.opalj.br.analyses.Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val executer = project.get(FPCFAnalysesManagerKey)

        var analysisTime = org.opalj.util.Seconds.None
        org.opalj.util.PerformanceEvaluation.time {

            executer.runAll(
                CallableFromClassesInOtherPackagesAnalysis,
                MethodAccessibilityAnalysis
            )

        } { t ⇒ analysisTime = t.toSeconds }

        val propertyStore = project.get(PropertyStoreKey)

        val global = entitiesByProperty(Global)(propertyStore)
        val packgeLocal = entitiesByProperty(PackageLocal)(propertyStore)
        val classLocal = entitiesByProperty(ClassLocal)(propertyStore)

        BasicReport(
            s"\nglobal            : ${global.size}"+
                s"\npackageLocal  : ${packgeLocal.size}"+
                s"\nclassLocal    : ${classLocal.size}"+
                "\nAnalysis time: "+analysisTime
        )
    }
}
