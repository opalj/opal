/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package br

import java.net.URL

import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project

import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.de.DependencyExtractor
import org.opalj.de.DependencyProcessorAdapter
import org.opalj.de.DependencyType

/**
 * Calculates the transitive closure of all classes referred to by a given class.
 * Here, referred to means that the type is explicitly used in the implementation
 * of the class.
 *
 * @author Michael Eichberg
 */
object TransitiveUsage extends AnalysisExecutor {

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
            dType: DependencyType): Unit = {
            def process(vse: VirtualSourceElement) {
                vse match {
                    case VirtualClass(declaringClassType) ⇒
                        processType(declaringClassType)
                    case VirtualField(declaringClassType, _, fieldType) ⇒
                        processType(declaringClassType)
                        processType(fieldType)
                    case VirtualMethod(declaringClassType, _, descriptor) ⇒
                        processType(declaringClassType)
                        processType(descriptor.returnType)
                        descriptor.parameterTypes.view foreach { processType(_) }
                }
            }
            process(source)
            process(target)
        }
    }

    val dependencyCollector = new DependencyExtractor(TypesCollector)

    override def analysisSpecificParametersDescription: String =
        "-class=<The class for which the transitive closure of used classes is determined>"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
        parameters.size == 1 && parameters.head.startsWith("-class=")

    override val analysis = new OneStepAnalysis[URL, BasicReport] {

        override val description: String =
            "Calculates the transitive closure of all classes used by a specific class. "+
                "(Does not take reflective usages into relation)."

        override def doAnalyze(
            project: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean) = {

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
                    visitedTypes.map(_.toJava).mkString("\n"))
        }
    }
}