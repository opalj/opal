/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.LBPure
import org.opalj.fpcf.properties.Purity
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Nanoseconds
import org.opalj.util.gc

/**
 * Runs the purity analysis including all analyses that may improve the overall result.
 *
 * @author Michael Eichberg
 */
object PurityAnalysisDemo extends DefaultOneStepAnalysis {

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
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        var r: () ⇒ String = null

        def handleResults(t: Nanoseconds, ts: Seq[Nanoseconds]) = {
            performanceData += ((t, List(setupTime, analysisTime)))
            performanceData = performanceData.filter((t_ts) ⇒ ts.contains(t_ts._1))
        }

        List(1, 2, 4, 8, 16, 32, 64).foreach { parallelismLevel ⇒
            performanceData = Map.empty

            println(s"\nRunning analysis with $parallelismLevel thread(s):")
            r = time[() ⇒ String](5, 10, 5, analyze(project, parallelismLevel))(handleResults)
            println(
                s"Results with $parallelismLevel threads:\n"+
                    performanceData.values.
                    map(v ⇒ v.map(_.toSeconds.toString(false))).
                    map(v ⇒ List("setup\t", "analysis\t").zip(v).map(e ⇒ e._1 + e._2).mkString("", "\n", "\n")).
                    mkString("\n")
            )

            gc()
        }

        BasicReport(r())
    }

    def analyze(theProject: Project[URL], parallelismLevel: Int): () ⇒ String = {
        val project = Project.recreate(theProject) // We need an empty project(!)

        import project.get
        // The following measurements (t) are done such that the results are comparable with the
        // reactive async approach developed by P. Haller and Simon Gries.

        val propertyStore = time {
            PropertyStoreKey.parallelismLevel = parallelismLevel
            get(PropertyStoreKey)
        } { r ⇒ setupTime = r }

        time {
            LazyL0FieldMutabilityAnalysis.startLazily(project, propertyStore)
            EagerL0PurityAnalysis.start(project, propertyStore)
            propertyStore.waitOnPhaseCompletion()
        } { r ⇒ analysisTime = r }

        println(s"\nsetup: ${setupTime.toSeconds}; analysis: ${analysisTime.toSeconds}")

        () ⇒ {
            val effectivelyFinalEntities: Iterator[EPS[Entity, FieldMutability]] =
                propertyStore.entities(FieldMutability.key)

            val effectivelyFinalFields: Iterator[(Field, Property)] =
                effectivelyFinalEntities.map(ep ⇒ (ep.e.asInstanceOf[Field], ep.ub))

            val effectivelyFinalFieldsAsStrings =
                effectivelyFinalFields.map(f ⇒ f._2+" >> "+f._1.toJava)

            val pureEntities: Iterator[EPS[Entity, Purity]] = propertyStore.entities(Purity.key)
            val pureMethods: Iterator[(Method, Property)] =
                pureEntities.map(eps ⇒ (eps.e.asInstanceOf[Method], eps.ub))
            val pureMethodsAsStrings = pureMethods.map(m ⇒ m._2+" >> "+m._1.toJava)

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
                "\nPure methods: "+pureMethods.filter(m ⇒ m._2 == LBPure).size
        }
    }
}
