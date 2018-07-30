/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import java.net.URL
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.ai.analyses.BaseFieldValuesAnalysisDomain
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile

/**
 * Demonstrates how to use OPAL's FieldTypesAnalysis.
 *
 * @author Michael Eichberg
 */
object FieldTypesAnalysis extends DefaultOneStepAnalysis {

    override def title: String =
        "tries to derive more precise information about the fields of a class"

    override def description: String =
        "Identifies fields of a class where we can – statically – derive more precise type/value information."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val refinedFieldValues =
            org.opalj.ai.analyses.FieldValuesAnalysis.doAnalyze(
                theProject,
                (project: SomeProject, classFile: ClassFile) ⇒
                    new BaseFieldValuesAnalysisDomain(project, classFile),
                isInterrupted
            )

        BasicReport(
            refinedFieldValues.seq.map { info ⇒
                val (field, fieldValue) = info
                val classFile = field.classFile
                classFile.thisType.toJava+
                    "{ "+
                    field.name+":"+fieldValue+
                    " // Originaltype: "+field.fieldType.toJava+
                    " }"
            }.mkString("\n")+
                "\n"+
                "Number of refined field types: "+refinedFieldValues.size+"\n"
        )
    }
}
