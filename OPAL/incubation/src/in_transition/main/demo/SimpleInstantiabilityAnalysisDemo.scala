/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package demo

import java.net.URL

import org.opalj.br.ClassFile
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.fpcf.properties.Instantiability
import org.opalj.fpcf.properties.Instantiable

/**
 * @author Michael Reif
 */
object SimpleInstantiabilityAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "class instantiablility computation"

    override def description: String = "determines the instantiable classes of a library/application"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val propertyStore = project.get(PropertyStoreKey)
        var analysisTime = org.opalj.util.Seconds.None

        org.opalj.util.PerformanceEvaluation.time {

            SimpleInstantiabilityAnalysis.run(project)

        } { t ⇒ analysisTime = t.toSeconds }

        val instantiableClasses: Traversable[EP[Entity, Instantiability]] =
            propertyStore.entities(Instantiability.key).filter { ep ⇒ ep.p == Instantiable }

        val classInfo = instantiableClasses.map { e ⇒
            val classFile = e._1.asInstanceOf[ClassFile]
            classFile.thisType.toJava
        }

        BasicReport(classInfo.mkString(
            "\ninstantiable classes:\n\n\t",
            "\n\t",
            s"\n# instantiable classes: ${instantiableClasses.size}\n"
        ) +
            s"\n #classes: ${project.classFilesCount}\n"+
            "\nanalysis time: "+analysisTime)
    }
}
