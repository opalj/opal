/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL

import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project

import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.de.DependencyExtractor
import org.opalj.de.DependencyProcessorAdapter
import org.opalj.de.DependencyType

/**
 * Calculates the transitive closure of all classes referred to by a given class.
 * Here, "referred to" means that the type is explicitly used in the implementation
 * of the class.
 *
 * @author Michael Eichberg
 */
object TransitiveUsage extends AnalysisApplication {

    private[this] var visitedTypes = Set.empty[ObjectType]

    // Types which are extracted but which are not yet analyzed.
    private[this] var extractedTypes = Set.empty[ObjectType]

    // To extract all usages we reuse the infrastructure that enables us to extract
    // dependencies. In this case we just record referred to types and do not actually
    // record the concrete dependencies.
    object TypesCollector extends DependencyProcessorAdapter {

        private def processType(t: Type): Unit =
            if (t.isObjectType) {
                val objectType = t.asObjectType
                if (!visitedTypes.contains(objectType))
                    extractedTypes += objectType
            }

        override def processDependency(
            source: VirtualSourceElement,
            target: VirtualSourceElement,
            dType:  DependencyType
        ): Unit = {
            def process(vse: VirtualSourceElement): Unit = {
                vse match {
                    case VirtualClass(declaringClassType) =>
                        processType(declaringClassType)
                    case VirtualField(declaringClassType, _, fieldType) =>
                        processType(declaringClassType)
                        processType(fieldType)
                    case VirtualMethod(declaringClassType, _, descriptor) =>
                        processType(declaringClassType)
                        processType(descriptor.returnType)
                        descriptor.parameterTypes.view foreach { processType(_) }
                    case VirtualForwardingMethod(_, _, _, _) | _: VirtualMethod => throw new MatchError(vse)
                }
            }
            process(source)
            process(target)
        }
    }

    val dependencyCollector = new DependencyExtractor(TypesCollector)

    override def analysisSpecificParametersDescription: String = {
        "-class=<The class for which the transitive closure of used classes is determined>"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Seq[String] = {
        if (parameters.size == 1 && parameters.head.startsWith("-class="))
            Seq.empty
        else
            parameters.filterNot(_.startsWith("-class=")).map("unknown parameter: "+_)
    }

    override val analysis = new OneStepAnalysis[URL, BasicReport] {

        override val description: String =
            "Calculates the transitive closure of all classes used by a specific class. "+
                "(Does not take reflective usages into relation)."

        override def doAnalyze(
            project:       Project[URL],
            parameters:    Seq[String],
            isInterrupted: () => Boolean
        ) = {

            val baseType = ObjectType(parameters.head.substring(7).replace('.', '/'))
            extractedTypes += baseType
            while (extractedTypes.nonEmpty) {
                val nextType = extractedTypes.head
                visitedTypes += nextType
                val nextClassFile = project.classFile(nextType)
                extractedTypes = extractedTypes.tail
                nextClassFile.foreach(dependencyCollector.process)
            }

            BasicReport(
                "To compile: "+baseType.toJava+
                    " the following "+visitedTypes.size+" classes are required:\n"+
                    visitedTypes.map(_.toJava).mkString("\n")
            )
        }
    }
}
