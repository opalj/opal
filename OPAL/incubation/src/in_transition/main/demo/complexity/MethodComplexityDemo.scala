/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package complexity

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.fpcf.properties.MethodComplexity

/**
 * Demonstrates how to use an analysis that was developed using the FPCF framework.
 *
 * @author Michael Eichberg
 */
object MethodComplexityDemo extends DefaultOneStepAnalysis {

    override def title: String = "assesses the complexity of methods"

    override def description: String =
        """|a very simple assessment of a method's complexity that primarily serves
           |the goal to make decisions about those methods that may be inlined""".stripMargin('|')

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val theProject = project
        implicit val propertyStore = theProject.get(PropertyStoreKey)

        val analysis = new MethodComplexityAnalysis
        propertyStore.scheduleEagerComputationsForEntities(project.allMethodsWithBody) { m ⇒ Result(m, analysis(m)) }
        propertyStore.waitOnPropertyComputationCompletion(true)
        println(propertyStore.toString)
        val ratings = propertyStore.entities { p ⇒
            p match {
                case MethodComplexity(c) if c < Int.MaxValue ⇒ true
                case _                                       ⇒ false

            }
        }
        BasicReport(ratings.mkString("\n", "\n", s"\n${ratings.size} simple methods found - Done."))
    }
}
