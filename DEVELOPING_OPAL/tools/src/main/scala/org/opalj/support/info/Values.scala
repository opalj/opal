/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import java.net.URL

import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.Field
import org.opalj.ai.fpcf.properties.FieldValue
import org.opalj.ai.fpcf.properties.MethodReturnValue

/**
 * Computes information regarding the values stored in fields and returned by methods.
 *
 * @author Michael Eichberg
 */
object Values extends ProjectAnalysisApplication {

    override def title: String = "Values stored in fields and returned by methods"

    override def description: String = {
        "Provides information about the values returned by methods and those stored in fields."
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        implicit val classHierarchy = project.classHierarchy

        val (ps, _) =
            PerformanceEvaluation.time {
                project.get(FPCFAnalysesManagerKey).runAll(
                    org.opalj.ai.fpcf.analyses.EagerLBFieldValuesAnalysis,
                    org.opalj.ai.fpcf.analyses.EagerLBMethodReturnValuesAnalysis
                )
            } { t =>
                OPALLogger.info(
                    "analysis progress",
                    s"finished in ${t.toSeconds} "
                )(project.logContext)
            }

        val fieldValues: List[EPS[Entity, FieldValue]] = ps.entities(FieldValue.key).toList

        val mFields =
            fieldValues
                .filter { eps =>
                    val m = eps.e.asInstanceOf[Field]
                    m.fieldType.isReferenceType && {
                        val fieldValue = eps.lb.value.asReferenceValue
                        fieldValue.isNull.isYes || fieldValue.upperTypeBound.size > 1 ||
                            m.fieldType != fieldValue.upperTypeBound.head
                    }
                }
                // we are deriving more precise lower bounds => eps.lb
                .map(eps => eps.e.asInstanceOf[Field].toJava(" => "+eps.lb.value.toString))
                .sorted
                .mkString("Field Values:\n\t", "\n\t", s"\n(Overall: ${fieldValues.size})")

        val methodReturnValues: List[EPS[Entity, MethodReturnValue]] = {
            ps.entities(MethodReturnValue.key).toList
        }

        val objectValuesReturningMethodsCount =
            project.allMethodsWithBody.filter(_.returnType.isObjectType).size

        val mMethods =
            methodReturnValues
                .filter { eps =>
                    val m = eps.e.asInstanceOf[Method]
                    m.returnType.isReferenceType && {
                        eps.lb.returnValue.isEmpty || {
                            val returnValue = eps.lb.returnValue.get.asReferenceValue
                            returnValue.isNull.isYes || returnValue.upperTypeBound.size > 1 ||
                                m.returnType != returnValue.upperTypeBound.head
                        }
                    }
                }
                // we are deriving more precise lower bounds => eps.lb
                .map(eps => eps.e.asInstanceOf[Method].toJava(" => "+eps.lb.returnValue))
                .sorted
                .mkString(
                    "Method Return Values:\n\t",
                    "\n\t",
                    s"\n(Overall: ${methodReturnValues.size}/$objectValuesReturningMethodsCount)"
                )
        BasicReport(mFields+"\n\n"+mMethods)

    }
}
