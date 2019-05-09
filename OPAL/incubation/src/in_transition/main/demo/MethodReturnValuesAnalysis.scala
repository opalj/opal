/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.net.URL

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.Method
import org.opalj.br.ClassFile
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.MethodSignature
import org.opalj.ai.analyses.{MethodReturnValuesAnalysis ⇒ TheMethodReturValuesAnalysis}
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.analyses.BaseMethodReturnValuesAnalysisDomain
import org.opalj.ai.analyses.FPMethodReturnValuesAnalysisDomain
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.analyses.FPFieldValuesAnalysisDomain
import org.opalj.ai.analyses.FieldValuesAnalysis
import org.opalj.ai.analyses.cg.CallGraphCache

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesAnalysis extends ProjectAnalysisApplication {

    override def title: String = TheMethodReturValuesAnalysis.title

    override def description: String = TheMethodReturValuesAnalysis.description

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        var fieldValueInformation = theProject.get(FieldValuesKey)

        var methodReturnValueInformation: MethodReturnValueInformation = time {
            def createDomain(ai: InterruptableAI[Domain], method: Method) = {
                new BaseMethodReturnValuesAnalysisDomain(
                    theProject, fieldValueInformation, ai, method
                )
            }
            TheMethodReturValuesAnalysis.doAnalyze(theProject, isInterrupted, createDomain)
        } { t ⇒ println(s"Analysis time: $t") }
        println("number of methods with refined returned types "+methodReturnValueInformation.size)

        var continueAnalysis = true
        val cache = new CallGraphCache[MethodSignature, scala.collection.Set[Method]](theProject)
        do {

            val mrva = time {
                def createDomain(ai: InterruptableAI[Domain], method: Method) = {
                    new FPMethodReturnValuesAnalysisDomain(
                        theProject,
                        fieldValueInformation, methodReturnValueInformation,
                        cache,
                        ai, method
                    )
                }
                TheMethodReturValuesAnalysis.doAnalyze(theProject, isInterrupted, createDomain)
            } { t ⇒ println(s"Method return values analysis time: $t") }

            val fvta = time {
                def createDomain(project: SomeProject, classFile: ClassFile) = {
                    new FPFieldValuesAnalysisDomain(
                        theProject,
                        fieldValueInformation, methodReturnValueInformation,
                        cache,
                        classFile
                    )
                }
                FieldValuesAnalysis.doAnalyze(theProject, createDomain, isInterrupted)
            } { t ⇒ println(s"Field value information analysis time: $t.") }

            println("number of fields with refined types "+fieldValueInformation.size)
            println("number of methods with refined returned types "+methodReturnValueInformation.size)

            continueAnalysis =
                mrva.size > methodReturnValueInformation.size ||
                    fvta.size > fieldValueInformation.size

            methodReturnValueInformation = mrva
            fieldValueInformation = fvta
        } while (continueAnalysis)

        val results =
            methodReturnValueInformation.map { result ⇒
                val (method, value) = result
                RefinedReturnType[Domain](theProject, method, value)
            }

        BasicReport(
            results.map(_.toString()).toSeq.sorted.mkString(
                "Methods with refined return types ("+results.size+"): \n", "\n", "\n"
            )
        )
    }
}

case class RefinedReturnType[D <: Domain](
        project:     SomeProject,
        method:      Method,
        refinedType: Option[D#DomainValue]
) {

    override def toString: String = {

        val returnType = method.descriptor.returnType
        val additionalInfo =
            refinedType match {
                case value @ TypeOfReferenceValue(_ /*utb*/ ) ⇒
                    if (returnType.isReferenceType && value.isValueASubtypeOf(returnType.asReferenceType).isNoOrUnknown)
                        s"the $refinedType is not a subtype of the declared type $returnType"
                    else if (returnType.isObjectType && project.classHierarchy.hasSubtypes(returnType.asObjectType).isNoOrUnknown)
                        s"the $returnType has no subtypes, but we were still able to refine the value $refinedType"
                case _ ⇒ ""
            }

        import Console._

        "Refined the return type of "+BOLD + BLUE +
            method.toJava+
            " => "+GREEN +
            refinedType.getOrElse("\"NONE\" (the method never returns normally)") +
            RESET + RED + additionalInfo + RESET
    }

}
