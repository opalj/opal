/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File

import org.opalj.ai.fpcf.properties.FieldValue
import org.opalj.ai.fpcf.properties.MethodReturnValue
import org.opalj.br.ClassHierarchy
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.MultiProjectAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.PropertyStoreBasedCommandLineConfig
import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation

import org.rogach.scallop.ScallopConf

/**
 * Computes information regarding the values stored in fields and returned by methods.
 *
 * @author Michael Eichberg
 */
object Values extends MultiProjectAnalysisApplication {

    protected class ValuesConfig(args: Array[String]) extends ScallopConf(args)
        with MultiProjectAnalysisConfig[ValuesConfig]
        with PropertyStoreBasedCommandLineConfig {
        banner("Collects information about values returned by methods or stored in fields\n")
    }

    protected type ConfigType = ValuesConfig

    protected def createConfig(args: Array[String]): ValuesConfig = new ValuesConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ValuesConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (ps, _) = analysisConfig.setupPropertyStore(project)

        PerformanceEvaluation.time {
            project.get(FPCFAnalysesManagerKey).runAll(
                org.opalj.ai.fpcf.analyses.EagerLBFieldValuesAnalysis,
                org.opalj.ai.fpcf.analyses.EagerLBMethodReturnValuesAnalysis
            )
        } { t => OPALLogger.info("analysis progress", s"finished in ${t.toSeconds} ")(project.logContext) }

        implicit val classHierarchy: ClassHierarchy = project.classHierarchy

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
                .map(eps => eps.e.asInstanceOf[Field].toJava(" => " + eps.lb.value.toString))
                .sorted
                .mkString("Field Values:\n\t", "\n\t", s"\n(Overall: ${fieldValues.size})")

        val methodReturnValues: List[EPS[Entity, MethodReturnValue]] = ps.entities(MethodReturnValue.key).toList

        val objectValuesReturningMethodsCount = project.allMethodsWithBody.count(_.returnType.isClassType)

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
                .map(eps => eps.e.asInstanceOf[Method].toJava(" => " + eps.lb.returnValue))
                .sorted
                .mkString(
                    "Method Return Values:\n\t",
                    "\n\t",
                    s"\n(Overall: ${methodReturnValues.size}/$objectValuesReturningMethodsCount)"
                )
        (project, BasicReport(mFields + "\n\n" + mMethods))
    }
}
