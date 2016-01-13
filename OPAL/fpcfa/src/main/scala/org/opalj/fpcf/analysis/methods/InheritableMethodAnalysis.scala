/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package analysis

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import scala.collection.mutable.ListBuffer

sealed trait Inheritable extends Property {

    final def key = Inheritable.key // All instances have to share the SAME key!
}

object Inheritable extends PropertyMetaInformation {

    final val key = PropertyKey.create("Inheritable", IsInheritable)
}

case object NotInheritable extends Inheritable { final val isRefineable: Boolean = false }

case object IsInheritable extends Inheritable { final val isRefineable: Boolean = false }

/**
 * This analysis determines whether a method can directly called by a client. This is in particular
 * important when analyzing libraries.
 *
 * ==Usage==
 * Use the [[FPCFAnalysesManagerKey]] to query the analysis manager of a project. You can run
 * the analysis afterwards as follows:
 * {{{
 *  val analysisManager = project.get(FPCFAnalysisManagerKey)
 *  analysisManager.run(InheritableMethodAnalysis)
 * }}}
 * For detailed information see the documentation of the analysis manager.
 *
 * The results of this analysis are stored in the property store of the project. You can receive
 * the results as follows:
 * {{{
 * val theProjectStore = theProject.get(SourceElementsPropertyStoreKey)
 * val instantiableClasses = theProjectStore.entities { (p: Property) ⇒
 * p == IsInheritable
 * }
 * }}}
 *
 * @note The need of this property comes for example from call-by-signature computations
 *
 * @note This analysis does not make sense for applications, since these will not be extended by the client.
 *
 *
 * @author Michael Reif
 */
class InheritableMethodAnalysis private (
        val project: SomeProject
) extends FPCFAnalysis {

    /**
     *
     * This determines for a method whether it could be inherited by a library client.
     * It should not be called if the current analysis mode is application-related.
     *
     */
    def determineInheritability(method: Method): PropertyComputationResult = {

        if (method.isPrivate)
            return ImmediateResult(method, NotInheritable);

        val classFile = project.classFile(method)

        if (classFile.isEffectivelyFinal)
            return ImmediateResult(method, NotInheritable);

        if (isClosedLibrary && method.isPackagePrivate)
            return ImmediateResult(method, NotInheritable);

        if (classFile.isPublic || isOpenLibrary)
            return ImmediateResult(method, IsInheritable);

        val classType = classFile.thisType
        val classHierarchy = project.classHierarchy
        val methodName = method.name
        val methodDescriptor = method.descriptor

        val subtypes = ListBuffer.empty ++= classHierarchy.directSubtypesOf(classType)
        while (subtypes.nonEmpty) {
            val subtype = subtypes.head
            project.classFile(subtype) match {
                case Some(subclass) if subclass.isClassDeclaration || subclass.isEnumDeclaration ⇒
                    if (subclass.findMethod(methodName, methodDescriptor).isEmpty)
                        if (subclass.isPublic) {
                            // the original method is now visible (and not shadowed)
                            return ImmediateResult(method, IsInheritable);
                        } else
                            subtypes ++= classHierarchy.directSubtypesOf(subtype)
                // we need to continue our search for a class that makes the method visible
                case None ⇒
                    // The type hierarchy is obviously not downwards closed; i.e.,
                    // the project configuration is rather strange!
                    return ImmediateResult(method, IsInheritable);
                case _ ⇒
            }
            subtypes -= subtype
        }

        val x = Array(2, 3, 5, 6L)
        x foreach { element: Long ⇒
            println(element)
        }

        ImmediateResult(method, NotInheritable)
    }
}

object InheritableMethodAnalysis extends FPCFAnalysisRunner {

    final def entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isAbstract ⇒ m
    }

    override def derivedProperties: Set[PropertyKind] = Set(IsExtensible)

    protected[analysis] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new InheritableMethodAnalysis(project)
        propertyStore <||< (entitySelector, analysis.determineInheritability)
        analysis
    }
}