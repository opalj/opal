/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.immutability.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.immutability.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.TypeImmutability
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL0FieldAssignabilityAnalysis
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Determines the immutability of the classes of a project.
 *
 * @author Michael Eichberg
 */
object ImmutabilityAnalysis extends ProjectAnalysisApplication {

    override def title: String = "Immutability Analysis"

    override def description: String = "determines the immutability of classes and types"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        import project.get

        // The following measurements (t) are done such that the results are comparable with the
        // reactive async approach developed by P. Haller and Simon Gries.
        var t = Seconds.None
        val ps = time {

            val ps = get(PropertyStoreKey)
            val derivedPKs = Set.empty ++
                EagerL0FieldAssignabilityAnalysis.derives.map(_.pk) ++
                EagerClassImmutabilityAnalysis.derives.map(_.pk) ++
                EagerTypeImmutabilityAnalysis.derives.map(_.pk)
            ps.setupPhase(derivedPKs)
            EagerL0FieldAssignabilityAnalysis.start(project, ps, null)
            EagerClassImmutabilityAnalysis.start(project, ps, null)
            EagerTypeImmutabilityAnalysis.start(project, ps, null)
            ps.waitOnPhaseCompletion()
            ps
        } { r => t = r.toSeconds }

        val immutableClasses =
            ps.entities(ClassImmutability.key).toSeq.filter(ep =>
                project.classHierarchy.isInterface(ep.e.asInstanceOf[ObjectType]).isNo
            ).groupBy { _.ub }.map { kv =>
                (
                    kv._1,
                    kv._2.toList.sortWith { (a, b) =>
                        val cfA = a.e.asInstanceOf[ObjectType]
                        val cfB = b.e.asInstanceOf[ObjectType]
                        cfA.toJava < cfB.toJava
                    }
                )
            }

        val immutableClassesPerCategory =
            immutableClasses.map(kv => "\t\t" + kv._1 + ": " + kv._2.size).toList.sorted.mkString("\n")

        val immutableTypes =
            ps.entities(TypeImmutability.key).toSeq.filter(ep =>
                project.classHierarchy.isInterface(ep.e.asInstanceOf[ObjectType]).isNo
            ).groupBy { _.ub }.map { kv => (kv._1, kv._2.size) }
        val immutableTypesPerCategory =
            immutableTypes.map(kv => "\t\t" + kv._1 + ": " + kv._2).toList.sorted.mkString("\n")

        val immutableClassesInfo =
            immutableClasses.values.flatten
                .filter(ep => project.classHierarchy.isInterface(ep.e.asInstanceOf[ObjectType]).isNo)
                .map { ep =>
                    ep.e.asInstanceOf[ObjectType].toJava +
                        " => " + ep.ub +
                        " => " + ps(ep.e, TypeImmutability.key).ub
                }
                .mkString("\tImmutability:\n\t\t", "\n\t\t", "\n")

        BasicReport(
            "\nImmutability Information:\n" +
                immutableClassesInfo +
                "\nSummary (w.r.t classes):\n" +
                "\tObject Immutability:\n" +
                immutableClassesPerCategory + "\n" +
                "\tType Immutability:\n" +
                immutableTypesPerCategory + "\n" +
                "\n" + ps.toString(false) + "\n" +
                "The overall analysis took: " + t
        )
    }
}
