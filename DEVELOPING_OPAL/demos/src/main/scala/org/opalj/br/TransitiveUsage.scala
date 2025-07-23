/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.language.postfixOps

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.ClassNameArg
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
object TransitiveUsage extends ProjectsAnalysisApplication {

    protected class TransitiveUsageConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description =
            "Calculates the transitive closure of all classes used by a specific class (not taking reflective usages into account)"

        args(ClassNameArg !)
    }

    protected type ConfigType = TransitiveUsageConfig

    protected def createConfig(args: Array[String]): TransitiveUsageConfig = new TransitiveUsageConfig(args)

    private[this] var visitedTypes = Set.empty[ClassType]

    // Types which are extracted but which are not yet analyzed.
    private[this] var extractedTypes = Set.empty[ClassType]

    // To extract all usages we reuse the infrastructure that enables us to extract
    // dependencies. In this case we just record referred to types and do not actually
    // record the concrete dependencies.
    object TypesCollector extends DependencyProcessorAdapter {

        private def processType(t: Type): Unit =
            if (t.isClassType) {
                val classType = t.asClassType
                if (!visitedTypes.contains(classType))
                    extractedTypes += classType
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

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: TransitiveUsageConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val result = new StringBuilder()
        for { className <- analysisConfig(ClassNameArg) } {
            val baseType = ClassType(className)
            extractedTypes += baseType
            while (extractedTypes.nonEmpty) {
                val nextType = extractedTypes.head
                visitedTypes += nextType
                val nextClassFile = project.classFile(nextType)
                extractedTypes = extractedTypes.tail
                nextClassFile.foreach(dependencyCollector.process)
            }
            result.append(
                "To compile: " + baseType.toJava +
                    " the following " + visitedTypes.size + " classes are required:\n" +
                    visitedTypes.map(_.toJava).mkString("\n")
            )
        }

        (project, BasicReport(result.toString()))
    }
}
