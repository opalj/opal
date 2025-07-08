/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.io.File
import java.net.URL

import org.rogach.scallop.ScallopConf

import org.opalj.ai.domain.Origins
import org.opalj.ai.domain.RecordLastReturnedValues
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * A very small analysis that identifies those methods that always return a value
 * that was passed as a parameter to the method; the self reference `this` is also treated
 * as a(n implicit) parameter.
 *
 * @author Michael Eichberg
 */
object MethodsThatAlwaysReturnAPassedParameter extends ProjectsAnalysisApplication {

    protected class MethodsReturningParameterConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Identifies methods that either always throw an exception or return a given parameter"
    }

    protected type ConfigType = MethodsReturningParameterConfig

    protected def createConfig(args: Array[String]): MethodsReturningParameterConfig =
        new MethodsReturningParameterConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodsReturningParameterConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val methods = for {
            classFile <- project.allClassFiles.par
            method <- classFile.methods
            if method.body.isDefined
            if method.descriptor.returnType.isReferenceType
            if (
                method.descriptor.parametersCount > 0 &&
                    method.descriptor.parameterTypes.exists(_.isReferenceType)
            ) || !method.isStatic
            result = BaseAI(
                method,
                // "in real code" a specially tailored domain should be used.
                new domain.l1.DefaultDomain(project.asInstanceOf[Project[URL]], method) with RecordLastReturnedValues
            )
            if result.domain.allReturnedValues.forall {
                case (_, Origins(os)) if os.forall(_ < 0) => true
                case _                                    => false
            }
        } yield {
            // collect the origin information
            val origins = result.domain.allReturnedValues.values.flatMap(result.domain.originsIterator(_).toList).toSet

            method.toJava + (
                if (origins.nonEmpty)
                    "; returned values: " + origins.mkString(",")
                else
                    "; throws an exception"
            )
        }

        val (throwsException, returnsParameter) = methods.partition { _.endsWith("exception") }
        val returnsParameterResult =
            returnsParameter.toList.sorted.mkString(
                s"Found ${returnsParameter.size} methods which always return a passed parameter (including this):\n",
                "\n",
                "\n"
            )
        val throwsExceptionResult =
            throwsException.toList.sorted.mkString(
                s"Found ${throwsException.size} methods which always throw an exception:\n",
                "\n",
                "\n"
            )
        (project, BasicReport(throwsExceptionResult + returnsParameterResult))
    }
}
