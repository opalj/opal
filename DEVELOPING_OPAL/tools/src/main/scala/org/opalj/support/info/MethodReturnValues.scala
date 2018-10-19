/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import java.net.URL

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.ai.fpcf.analyses.EagerLBMethodReturnValuesAnalysis
import org.opalj.ai.fpcf.properties.MethodReturnValue

/**
 * Computes information regarding the values returned by methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValues extends DefaultOneStepAnalysis {

    override def title: String = "Method Return Values"

    override def description: String = {
        "Provides information about the values returned by methods."
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val ps = project.get(FPCFAnalysesManagerKey).runAll(
            EagerLBMethodReturnValuesAnalysis
        )

        val methodReturnValues: List[EPS[Entity, MethodReturnValue]] = {
            ps.entities(MethodReturnValue.key).toList
        }

        val m =
            methodReturnValues
                // we are deriving more precise lower bounds!
                .map(eps ⇒ eps.e.asInstanceOf[Method].toJava(" => "+eps.lb.returnValue))
                .sorted
                .mkString(
                    "Method Return Values:\n\t",
                    "\n\t",
                    s"\n(Overall: ${methodReturnValues.size}/${project.allMethodsWithBody.filter(_.returnType.isObjectType).size})"
                )
        BasicReport(m)
    }
}
