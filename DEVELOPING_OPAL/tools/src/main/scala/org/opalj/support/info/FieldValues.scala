/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.br.Field
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.ai.fpcf.analyses.EagerLBFieldValuesAnalysis
import org.opalj.ai.fpcf.properties.FieldValue

/**
 * Computes information regarding the values stored in the fields.
 *
 * @author Michael Eichberg
 */
object FieldValues extends DefaultOneStepAnalysis {

    override def title: String = "Field Values"

    override def description: String = {
        "Provides information about the values stored in fields."
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val ps = project.get(FPCFAnalysesManagerKey).runAll(
            EagerLBFieldValuesAnalysis
        )

        val fieldValues: List[EPS[Entity, FieldValue]] = ps.entities(FieldValue.key).toList

        val m =
            fieldValues
                // we are deriving more precise lower bounds!
                .map(eps ⇒ eps.e.asInstanceOf[Field].toJava(" => "+eps.lb.value.toString))
                .sorted
                .mkString("Field Values:\n\t", "\n\t", s"\n(Overall: ${fieldValues.size})")
        BasicReport(m)
    }
}
