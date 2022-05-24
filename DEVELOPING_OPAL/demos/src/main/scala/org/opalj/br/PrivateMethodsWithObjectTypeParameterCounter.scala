/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.BasicReport

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Counts the number of private methods that have at least one parameter with
 * a reference type.
 *
 * @author Michael Eichberg
 */
object PrivateMethodsWithObjectTypeParameterCounter extends ProjectAnalysisApplication {

    override def description: String = {
        "counts the number of package private and private methods "+
            "with a body with at least one parameter that is an object type"
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        val overallPotential = new java.util.concurrent.atomic.AtomicInteger(0)
        val methods = (
            for {
                classFile <- project.allClassFiles.par
                method <- classFile.methods
                if method.isPrivate //|| method.isPackagePrivate
                if method.name != "readObject" && method.name != "writeObject"
                potential = (method.descriptor.parameterTypes.collect {
                    case ot: ObjectType => project.classHierarchy.allSubtypes(ot, false).size
                    case _              => 0
                }).sum
                if potential >= 5
            } yield {
                overallPotential.addAndGet(potential)
                method.toJava(s" /* Potential: $potential */ ")
            }
        ).seq

        BasicReport(
            methods.mkString(
                "Methods:\n\t",
                "\n\t",
                s"\n\t${methods.size} methods found with an overall refinement potential of ${overallPotential.get}.\n"
            )
        )
    }
}
