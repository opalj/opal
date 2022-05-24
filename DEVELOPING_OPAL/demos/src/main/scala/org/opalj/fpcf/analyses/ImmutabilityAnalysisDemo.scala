/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.util.gc
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.ClassFile
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldMutabilityAnalysis

/**
 * Determines the immutability of the classes of a project.
 *
 * @author Michael Eichberg
 */
object ImmutabilityAnalysisDemo extends ProjectAnalysisApplication {

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//
    //                                                                                            //
    // THIS CODE CONTAINS THE PERFORMANCE MEASUREMENT CODE AS USED FOR THE "REACTIVE PAPER"!      //
    //                                                                                            //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//

    override def title: String = "determines the immutability of objects and types"

    override def description: String = "determines the immutability of objects and types"

    private[this] val setupTime = Nanoseconds.None
    private[this] var analysisTime = Nanoseconds.None
    private[this] var performanceData: Map[Nanoseconds, List[Nanoseconds]] = Map.empty

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        var r: () => String = null

        def handleResults(t: Nanoseconds, ts: Seq[Nanoseconds]) = {
            performanceData += ((t, List(setupTime, analysisTime)))
            performanceData = performanceData.filter((t_ts) => ts.contains(t_ts._1))
        }

        List(1, 2, 4, 8, 16, 32, 64).foreach { parallelismLevel =>
            performanceData = Map.empty
            gc()

            println(s"\nRunning analysis with $parallelismLevel thread(s):")
            r = time[() => String](10, 50, 15, analyze(project, parallelismLevel))(handleResults)
            println(
                s"Results with $parallelismLevel threads:\n"+
                    performanceData.values.
                    map(v => v.map(_.toSeconds.toString(false))).
                    map(v => List("setup\t", "analysis\t").zip(v).map(e => e._1 + e._2).mkString("", "\n", "\n")).
                    mkString("\n")
            )

            gc()
        }
        BasicReport(r())
    }

    def analyze(theProject: Project[URL], parallelismLevel: Int): () => String = {
        var result = "Results:\n"
        val project = Project.recreate(theProject) // We need an empty project(!)

        // The following measurements (t) are done such that the results are comparable with the
        // reactive async approach developed by P. Haller and Simon Gries.
        PropertyStoreKey.parallelismLevel = parallelismLevel
        val propertyStore = project.get(PropertyStoreKey)

        time {
            propertyStore.setupPhase(Set[PropertyKind](
                FieldMutability.key, ClassImmutability.key, TypeImmutability.key
            ))
            LazyL0FieldMutabilityAnalysis.register(project, propertyStore, null)
            EagerClassImmutabilityAnalysis.start(project, propertyStore, null)
            EagerTypeImmutabilityAnalysis.start(project, propertyStore, null)

            propertyStore.waitOnPhaseCompletion()
        } { r => analysisTime = r }

        result += s"\t- analysis time: ${analysisTime.toSeconds}\n"

        () => {
            val immutableClasses =
                propertyStore.entities(ClassImmutability.key).
                    filter(eps => !eps.e.asInstanceOf[ClassFile].isInterfaceDeclaration).toBuffer.
                    groupBy((eps: EPS[_ <: Entity, _ <: Property]) => eps.ub).
                    map { kv =>
                        (
                            kv._1,
                            kv._2.toList.sortWith { (a, b) =>
                                val cfA = a.e.asInstanceOf[ClassFile]
                                val cfB = b.e.asInstanceOf[ClassFile]
                                cfA.thisType.toJava < cfB.thisType.toJava
                            }
                        )
                    }

            val immutableClassesPerCategory =
                immutableClasses.
                    map(kv => "\t\t"+kv._1+": "+kv._2.size).
                    toBuffer.sorted.
                    mkString("\n")

            val immutableTypes =
                propertyStore.entities(TypeImmutability.key).
                    filter(eps => !eps.e.asInstanceOf[ClassFile].isInterfaceDeclaration).toBuffer.
                    groupBy((eps: EPS[_ <: Entity, _ <: Property]) => eps.ub).
                    map(kv => (kv._1, kv._2.size))
            val immutableTypesPerCategory =
                immutableTypes.map(kv => "\t\t"+kv._1+": "+kv._2).toBuffer.sorted.mkString("\n")

            val immutableClassesInfo =
                immutableClasses.values.flatten.filter { ep =>
                    !ep.e.asInstanceOf[ClassFile].isInterfaceDeclaration
                }.map { eps =>
                    eps.e.asInstanceOf[ClassFile].thisType.toJava+
                        " => "+eps.ub+
                        " => "+propertyStore(eps.e, TypeImmutability.key).ub
                }.mkString("\t\timmutability:\n\t\t", "\n\t\t", "\n")

            "\t- details:\n"+
                immutableClassesInfo+
                "\nSummary (w.r.t classes):\n"+
                "\tObject Immutability:\n"+
                immutableClassesPerCategory+"\n"+
                "\tType Immutability:\n"+
                immutableTypesPerCategory+"\n"+
                "\n"+propertyStore.toString(false)
        }
    }
}
