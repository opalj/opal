/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import java.net.URL

import org.opalj.br.ClassFile
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.EagerL0FieldImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyUnsoundPrematurelyReadFieldsAnalysis
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.ClassImmutability_new
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.FieldPrematurelyRead
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.ReferenceImmutability
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.br.fpcf.properties.TypeImmutability_new
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKind
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerL0ReferenceImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.EagerLxClassImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.EagerLxTypeImmutabilityAnalysis_new
import org.opalj.tac.fpcf.analyses.LazyL2FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.gc

/**
 * Determines the immutability of the classes of a project.
 *
 * @author Michael Eichberg
 *         @author Tobias Peter Roth
 *
 */
object ImmutabilityAnalysisDemo_new extends ProjectAnalysisApplication {

    override def title: String = "determines the immutability of objects and types"

    override def description: String = "determines the immutability of objects and types"

    private[this] var setupTime = Nanoseconds.None
    private[this] var analysisTime = Nanoseconds.None
    private[this] var performanceData: Map[Nanoseconds, List[Nanoseconds]] = Map.empty

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        var r: () ⇒ String = null

        def handleResults(t: Nanoseconds, ts: Seq[Nanoseconds]) = {
            performanceData += ((t, List(setupTime, analysisTime)))
            performanceData = performanceData.filter((t_ts) ⇒ ts.contains(t_ts._1))
        }

        List(1).foreach { parallelismLevel ⇒
            performanceData = Map.empty
            gc()

            println(s"\nRunning analysis with $parallelismLevel thread(s):")
            r = time[() ⇒ String](10, 50, 15, analyze(project, parallelismLevel))(handleResults)
            println(
                s"Results with $parallelismLevel threads:\n"+
                    performanceData.values
                    .map(v ⇒ v.map(_.toSeconds.toString(false)))
                    .map(
                        v ⇒ List("setup\t", "analysis\t").zip(v).map(e ⇒ e._1 + e._2).mkString("", "\n", "\n")
                    )
                    .mkString("\n")
            )

            gc()
        }
        BasicReport(r())
    }

    def analyze(theProject: Project[URL], parallelismLevel: Int): () ⇒ String = {
        var result = "Results:\n"
        val project = Project.recreate(theProject) // We need an empty project(!)
        project.get(RTACallGraphKey)

        // The following measurements (t) are done such that the results are comparable with the
        // reactive async approach developed by P. Haller and Simon Gries.
        PropertyStoreKey.parallelismLevel = parallelismLevel
        //PropertyStoreKey
        val propertyStore = project.get(PropertyStoreKey)

        time {
            propertyStore.setupPhase(
                Set[PropertyKind](
                    FieldMutability.key,
                    ClassImmutability.key,
                    TypeImmutability.key,
                    FieldPrematurelyRead.key,
                    Purity.key,
                    FieldImmutability.key,
                    ReferenceImmutability.key,
                    ClassImmutability_new.key,
                    TypeImmutability_new.key
                )
            )
            //LazyL0FieldMutabilityAnalysis.register(project, propertyStore, null) // (project, propertyStore)
            //EagerClassImmutabilityAnalysis.start(project, propertyStore, project.allClassFiles)
            //EagerTypeImmutabilityAnalysis.start(project, propertyStore, null) //project.allClassFiles)

            LazyClassImmutabilityAnalysis.register(project, propertyStore, project.allClassFiles)
            LazyL2FieldMutabilityAnalysis.register(project, propertyStore, null)
            LazyTypeImmutabilityAnalysis.register(project, propertyStore, null)
            LazyUnsoundPrematurelyReadFieldsAnalysis.register(project, propertyStore, null)
            LazyL2PurityAnalysis.register(project, propertyStore, null)

            EagerL0ReferenceImmutabilityAnalysis.start(project, propertyStore, null)
            EagerL0FieldImmutabilityAnalysis.start(project, propertyStore, null)
            EagerLxClassImmutabilityAnalysis_new.start(project, propertyStore, project.allClassFiles)
            EagerLxTypeImmutabilityAnalysis_new.start(project, propertyStore, null)
            //propertyStore.suppressError = true
            //propertyStore.waitOnPhaseCompletion()
        } { r ⇒
            analysisTime = r
        }

        result += s"\t- analysis time: ${analysisTime.toSeconds}\n"

        () ⇒ {
            val immutableReferences =
                propertyStore.entities(ReferenceImmutability.key)
            val immutableClasses =
                propertyStore
                    .entities(ClassImmutability_new.key)
                    .filter(eps ⇒ !eps.e.asInstanceOf[ClassFile].isInterfaceDeclaration)
                    .toBuffer
                    .groupBy((eps: EPS[_ <: Entity, _ <: Property]) ⇒ eps.ub)
                    .map { kv ⇒
                        (
                            kv._1,
                            kv._2.toList.sortWith { (a, b) ⇒
                                val cfA = a.e.asInstanceOf[ClassFile]
                                val cfB = b.e.asInstanceOf[ClassFile]
                                cfA.thisType.toJava < cfB.thisType.toJava
                            }
                        )
                    }

            val immutableClassesPerCategory =
                immutableClasses
                    .map(kv ⇒ "\t\t"+kv._1+": "+kv._2.size)
                    .toBuffer
                    .sorted
                    .mkString("\n")

            val immutableTypes =
                propertyStore
                    .entities(TypeImmutability_new.key)
                    .filter(eps ⇒ !eps.e.asInstanceOf[ClassFile].isInterfaceDeclaration)
                    .toBuffer
                    .groupBy((eps: EPS[_ <: Entity, _ <: Property]) ⇒ eps.ub)
                    .map(kv ⇒ (kv._1, kv._2.size))
            val immutableTypesPerCategory =
                immutableTypes.map(kv ⇒ "\t\t"+kv._1+": "+kv._2).toBuffer.sorted.mkString("\n")

            val immutableClassesInfo =
                immutableClasses.values.flatten
                    .filter { ep ⇒
                        !ep.e.asInstanceOf[ClassFile].isInterfaceDeclaration
                    }
                    .map { eps ⇒
                        eps.e.asInstanceOf[ClassFile].thisType.toJava+
                            " => "+eps.ub+
                            " => "+propertyStore(eps.e, TypeImmutability_new.key).ub
                    }
                    .mkString("\t\timmutability:\n\t\t", "\n\t\t", "\n")

            "immutable References: "+immutableReferences.size+"\n"
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
