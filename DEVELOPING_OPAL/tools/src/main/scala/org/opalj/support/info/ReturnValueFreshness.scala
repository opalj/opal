/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.properties.ExtensibleGetter
import org.opalj.br.fpcf.properties.FreshReturnValue
import org.opalj.br.fpcf.properties.Getter
import org.opalj.br.fpcf.properties.NoFreshReturnValue
import org.opalj.br.fpcf.properties.PrimitiveReturnValue
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.EagerReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis

/**
 * Computes return value freshness information; see
 * [[org.opalj.br.fpcf.properties.ReturnValueFreshness]] for details.
 *
 * @author Florian Kuebler
 */
object ReturnValueFreshness extends ProjectAnalysisApplication {

    override def title: String = "\"Freshness\" of Return Values"

    override def description: String = {
        "Describes whether a method returns a value that is allocated in that method or its "+
            "callees and only has escape state EscapeViaReturn"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        project.get(RTACallGraphKey)

        val (ps, _ /*executed analyses*/ ) = project.get(FPCFAnalysesManagerKey).runAll(
            LazyInterProceduralEscapeAnalysis,
            LazyFieldLocalityAnalysis,
            EagerReturnValueFreshnessAnalysis
        )

        val fresh = ps.finalEntities(FreshReturnValue).toSeq
        val notFresh = ps.finalEntities(NoFreshReturnValue).toSeq
        val prim = ps.finalEntities(PrimitiveReturnValue).toSeq
        val getter = ps.finalEntities(Getter).toSeq
        val extGetter = ps.finalEntities(ExtensibleGetter).toSeq

        val message =

            s"""|${fresh.mkString("fresh methods:", "\t\n)}", "")}
                |${getter.mkString("getter methods:", "\t\n)}", "")}
                |${extGetter.mkString("external getter methods:", "\t\n)}", "")}
                |${prim.mkString("methods with primitive return value:", "\t\n)}", "")}
                |${notFresh.mkString("methods that are not fresh at all:", "\t\n)}", "")}
                |# of methods with fresh return value: ${fresh.size}
                |# of methods without fresh return value: ${notFresh.size}
                |# of methods with primitive return value: ${prim.size}
                |# of methods that are getters: ${getter.size}
                |# of methods that are extensible getters: ${extGetter.size}
                |"""

        BasicReport(message.stripMargin('|'))
    }
}
