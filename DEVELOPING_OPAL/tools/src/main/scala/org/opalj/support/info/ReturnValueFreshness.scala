/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.EagerReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.properties.ExtensibleGetter
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.Getter
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.VExtensibleGetter
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VGetter
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VPrimitiveReturnValue

/**
 * Computes return value freshness information; see
 * [[org.opalj.fpcf.properties.ReturnValueFreshness]] for details.
 *
 * @author Florian Kuebler
 */
object ReturnValueFreshness extends DefaultOneStepAnalysis {

    override def title: String = "\"Freshness\" of Return Values"

    override def description: String = {
        "Describes whether a method returns a value that is allocated in that method or its "+
            "callees and only has escape state \"Escape Via Return\""
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val ps = project.get(FPCFAnalysesManagerKey).runAll(
            LazyInterProceduralEscapeAnalysis,
            LazyFieldLocalityAnalysis,
            LazyVirtualCallAggregatingEscapeAnalysis,
            LazyVirtualReturnValueFreshnessAnalysis,
            EagerReturnValueFreshnessAnalysis
        )

        // TODO Provide more useful information about the entities and then add tests

        val fresh = ps.finalEntities(FreshReturnValue).toSeq
        val notFresh = ps.finalEntities(NoFreshReturnValue).toSeq
        val prim = ps.finalEntities(PrimitiveReturnValue).toSeq
        val getter = ps.finalEntities(Getter).toSeq
        val extGetter = ps.finalEntities(ExtensibleGetter).toSeq
        val vfresh = ps.finalEntities(VFreshReturnValue).toSeq
        val vnotFresh = ps.finalEntities(VNoFreshReturnValue).toSeq
        val vprim = ps.finalEntities(VPrimitiveReturnValue).toSeq
        val vgetter = ps.finalEntities(VGetter).toSeq
        val vextGetter = ps.finalEntities(VExtensibleGetter).toSeq

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
                |# of vmethods with fresh return value: ${vfresh.size}
                |# of vmethods without fresh return value: ${vnotFresh.size}
                |# of vmethods with primitive return value: ${vprim.size}
                |# of vmethods that are getters: ${vgetter.size}
                |# of vmethods that are extensible getters: ${vextGetter.size}
                |"""

        BasicReport(message.stripMargin('|'))
    }
}
