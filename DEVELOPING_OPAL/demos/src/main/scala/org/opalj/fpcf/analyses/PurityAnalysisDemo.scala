/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.gc
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.Field
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.EagerL0PurityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0FieldMutabilityAnalysis
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.Purity

/**
 * Runs the purity analysis including all analyses that may improve the overall result.
 *
 * @author Michael Eichberg
 */
object PurityAnalysisDemo extends ProjectAnalysisApplication {

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//
    //                                                                                            //
    // THIS CODE CONTAINS THE PERFORMANCE MEASUREMENT CODE AS USED FOR THE "REACTIVE PAPER"!      //
    //                                                                                            //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!//

    override def title: String = "determines those methods that are pure"

    override def description: String = {
        "identifies methods which are pure; i.e. which just operate on the passed parameters"
    }

    private[this] var setupTime = Nanoseconds.None
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

            println(s"\nRunning analysis with $parallelismLevel thread(s):")
            r = time[() => String](5, 10, 5, analyze(project, parallelismLevel))(handleResults)
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
        val project = Project.recreate(theProject) // We need an empty project(!)

        import project.get
        // The following measurements (t) are done such that the results are comparable with the
        // reactive async approach developed by P. Haller and Simon Gries.

        val propertyStore = time {
            PropertyStoreKey.parallelismLevel = parallelismLevel
            get(PropertyStoreKey)
        } { r => setupTime = r }

        time {
            LazyL0FieldMutabilityAnalysis.register(project, propertyStore, null)
            EagerL0PurityAnalysis.start(project, propertyStore, null)
            propertyStore.waitOnPhaseCompletion()
        } { r => analysisTime = r }

        println(s"\nsetup: ${setupTime.toSeconds}; analysis: ${analysisTime.toSeconds}")

        () => {
            val effectivelyFinalEntities: Iterator[EPS[Entity, FieldMutability]] =
                propertyStore.entities(FieldMutability.key)

            val effectivelyFinalFields: Iterator[(Field, Property)] =
                effectivelyFinalEntities.map(ep => (ep.e.asInstanceOf[Field], ep.ub))

            val effectivelyFinalFieldsAsStrings =
                effectivelyFinalFields.map(f => s"${f._2} >> ${f._1.toJava}")

            val pureEntities: Iterator[EPS[Entity, Purity]] = propertyStore.entities(Purity.key)
            val pureMethods: Iterator[(DefinedMethod, Property)] =
                pureEntities.map(eps => (eps.e.asInstanceOf[DefinedMethod], eps.ub))
            val pureMethodsAsStrings = pureMethods.map(m => s"${m._2} >> ${m._1.toJava}")

            val fieldInfo =
                effectivelyFinalFieldsAsStrings.toList.sorted.mkString(
                    "\nMutability of private static non-final fields:\n",
                    "\n",
                    s"\nTotal: ${effectivelyFinalFields.size}\n"
                )

            val methodInfo =
                pureMethodsAsStrings.toList.sorted.mkString(
                    "\nPure methods:\n",
                    "\n",
                    s"\nTotal: ${pureMethods.size}\n"
                )

            fieldInfo + methodInfo + propertyStore.toString(false)+
                "\nPure methods: "+pureMethods.filter(m => m._2 == Pure).size
        }
    }
}
