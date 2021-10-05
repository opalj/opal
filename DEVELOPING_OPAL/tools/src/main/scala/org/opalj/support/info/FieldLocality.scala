/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.properties.ExtensibleLocalField
import org.opalj.br.fpcf.properties.ExtensibleLocalFieldWithGetter
import org.opalj.br.fpcf.properties.LocalField
import org.opalj.br.fpcf.properties.LocalFieldWithGetter
import org.opalj.br.fpcf.properties.NoLocalField
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.EagerFieldLocalityAnalysis

/**
 * Computes the field locality; see [[org.opalj.br.fpcf.properties.FieldLocality]] for details.
 *
 * @author Florian Kuebler
 */
object FieldLocality extends ProjectAnalysisApplication {

    override def title: String = "Field Locality"

    override def description: String = {
        "Provides lifetime information about the values stored in instance fields."
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        project.get(RTACallGraphKey)

        val (ps, _ /*executed analyses*/ ) = project.get(FPCFAnalysesManagerKey).runAll(
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            EagerFieldLocalityAnalysis
        )

        val local = ps.finalEntities(LocalField).toSeq
        val nolocal = ps.finalEntities(NoLocalField).toSeq
        val extLocal = ps.finalEntities(ExtensibleLocalField).toSeq
        val getter = ps.finalEntities(LocalFieldWithGetter).toSeq
        val extGetter = ps.finalEntities(ExtensibleLocalFieldWithGetter).toSeq

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
