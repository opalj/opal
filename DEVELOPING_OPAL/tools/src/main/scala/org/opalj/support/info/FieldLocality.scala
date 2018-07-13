/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.EagerFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.properties.ExtensibleLocalField
import org.opalj.fpcf.properties.ExtensibleLocalFieldWithGetter
import org.opalj.fpcf.properties.LocalField
import org.opalj.fpcf.properties.LocalFieldWithGetter
import org.opalj.fpcf.properties.NoLocalField

/**
 * Computes the field locality; see [[org.opalj.fpcf.properties.FieldLocality]] for details.
 *
 * @author Florian Kuebler
 */
object FieldLocality extends DefaultOneStepAnalysis {

    override def title: String = "Field Locality"

    override def description: String = {
        "Provides lifetime information about the values stored in instance fields."
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val ps = project.get(FPCFAnalysesManagerKey).runAll(
            LazyInterProceduralEscapeAnalysis,
            LazyVirtualCallAggregatingEscapeAnalysis,
            LazyVirtualReturnValueFreshnessAnalysis,
            LazyReturnValueFreshnessAnalysis,
            EagerFieldLocalityAnalysis
        )

        val local = ps.finalEntities(LocalField).toSeq
        val nolocal = ps.finalEntities(NoLocalField).toSeq
        val extLocal = ps.finalEntities(ExtensibleLocalField).toSeq
        val getter = ps.finalEntities(LocalFieldWithGetter).toSeq
        val extGetter = ps.finalEntities(ExtensibleLocalFieldWithGetter).toSeq

        // TODO Provide more useful information about the entities and then add tests

        val message =
            s"""|# of local fields: ${local.size}
                |# of not local fields: ${nolocal.size}
                |# of extensible local fields: ${extLocal.size}
                |# of local fields with getter: ${getter.size}
                |# of extensible local fields with getter: ${extGetter.size}
                |"""

        BasicReport(message.stripMargin('|'))
    }
}
