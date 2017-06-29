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
package org.opalj.fpcf.analysis

import org.opalj.br._
import org.opalj.br.analyses.{AllocationSites, PropertyStoreKey}
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.properties.{EscapeProperty, NoEscape}

class SimpleEscapeAnalysisTest extends AbstractFixpointAnalysisTest {

    def analysisName = "SimpleEscapeAnalysis"

    override def testFileName = "escape-1.8-g-parameters-genericsignature.jar"

    override def testFilePath = "bi"

    override def analysisRunner = SimpleEscapeAnalysis

    override def propertyKey: PropertyKey[EscapeProperty] = EscapeProperty.key

    override def propertyAnnotation: ObjectType = {
        ObjectType("annotations/target/EscapeProperty")
    }

    /**
      * Add all AllocationsSites found in the project to the entities in the property
      * stores created with the PropertyStoreKey.
      */
    override def init(): Unit = {
        PropertyStoreKey.makeAllocationSitesAvailable(project)
    }

    def defaultValue = NoEscape.toString

    def propertyExtraction(annotation: TypeAnnotation): Option[String] = {
        annotation.elementValuePairs collectFirst { case ElementValuePair("value", EnumValue(_, property)) ⇒ property }
    }

    def validatePropertyByTypeAnnotation(m: Method, annon: TypeAnnotation, pcToAs: Map[PC, AllocationSite]): Unit = {
        val annotatedOProperty = propertyExtraction(annon)
        val annotatedProperty = annotatedOProperty getOrElse defaultValue

        assert(m ne null, "method is empty")

        val expr = annon.target match {
            case TAOfNew(pc) ⇒ Some(pcToAs(pc))

            case TAOfFormalParameter(_) ⇒
                throw new RuntimeException("Not yet implemented")
            case _ ⇒ None
        }

        expr.foreach(entity => {
            val computedOProperty = propertyStore(entity, propertyKey)

            if (computedOProperty.hasNoProperty) {
                val className = project.classFile(m).fqn
                val message =
                    "Entity has no property: " + s"$className $m $entity  for: $propertyKey;" +
                        s"\nexpected property: $annotatedProperty"
                fail(message)
            }

            val computedProperty = computedOProperty.p.toString

            if (computedProperty != annotatedProperty) {
                val className = project.classFile(m).fqn
                val message =
                    "Wrong property computed: " +
                        s"$className $m $entity" +
                        s"has the property $computedProperty for $propertyKey;" +
                        s"\n\tactual property:   $computedProperty" +
                        s"\n\texpected property: $annotatedProperty"
                fail(message)
            }

        })
    }

    // TEST

    for {
        classFile ← project.allClassFiles
        method@MethodWithBody(code) ← classFile.methods
        annotation ← code.runtimeVisibleTypeAnnotations
        if annotation.annotationType == propertyAnnotation
    } {
        val allocationSites = propertyStore.context[AllocationSites]
        analysisName should ("correctly calculate the property of the expression " +
            annotation.target + "in method " + method.name + " in class " + classFile.fqn) in {
            validatePropertyByTypeAnnotation(method, annotation, allocationSites(method))
        }
    }

}
