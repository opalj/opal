/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved

import analyses.{ Analysis, AnalysisExecutor, BasicReport, Project }
import dependency._

import java.net.URL

/**
 * Calculates the transitive closure of all classes referred to by a given set of classes.
 *
 * @author Michael Eichberg
 */
object TransitiveUsage extends AnalysisExecutor {

    var visitedTypes = Set.empty[ObjectType]

    var extractedTypes = Set.empty[ObjectType]

    object TypesCollector extends SourceElementIDs {

        def processType(t: Type) {
            if (t.isObjectType) {
                val objectType = t.asObjectType
                if (!visitedTypes.contains(objectType))
                    extractedTypes += objectType
            }
        }

        def sourceElementID(
            t: Type): Int = {

            processType(t)
            -1
        }

        def sourceElementID(
            definingObjectType: ObjectType,
            fieldName: String): Int = {

            processType(definingObjectType)
            -1
        }

        def sourceElementID(
            definingReferenceType: ReferenceType,
            methodName: String,
            methodDescriptor: MethodDescriptor): Int = {

            processType(definingReferenceType)
            -1
        }
    }

    val dependencyCollector =
        new DependencyExtractor(TypesCollector) with NoSourceElementsVisitor {

            def processDependency(
                sourceID: Int,
                targetID: Int,
                dependencyType: DependencyType) {
                /* Nothing to do... */
            }
        }

    override def analysisParametersDescription: String =
        "-class=<The class for which the transitive closure of used classes is determined>"

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Boolean =
        parameters.size == 1 && parameters.head.startsWith("-class=")

    val analysis = new Analysis[URL, BasicReport] {

        def description: String =
            "Calculates the transitive closure of all classes used by a specific class. "+
                "(Does not take reflective usages into relation)."

        def analyze(project: Project[URL], parameters: Seq[String]) = {

            val baseType = ObjectType(parameters.head.substring(7).replace('.', '/'))
            extractedTypes += baseType
            while (extractedTypes.nonEmpty) {
                val nextType = extractedTypes.head
                visitedTypes += nextType
                val nextClassFile = project(nextType)
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