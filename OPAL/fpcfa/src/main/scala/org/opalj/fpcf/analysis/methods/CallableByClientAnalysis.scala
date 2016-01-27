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
package methods

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import scala.collection.mutable

/**
 * Deterrmines for each method if it potentially can be inherit by a '''future'''
 * subtype.
 *
 * @note This property is computed by a direct property computation.
 *
 * @author Michael Reif
 */
sealed trait ClientCallable extends Property {

    final type Self = ClientCallable

    final def key = ClientCallableKey

    final def isRefineable = false
}

case object IsClientCallable extends ClientCallable

case object NotClientCallable extends ClientCallable

/**
 * This analysis computes the ClientCallable property.* I.e., it determines whether a method can directly called by a client. This is in particular
 * important when analyzing libraries.
 *
 * ==Usage==
 * Use the [[FPCFAnalysesManagerKey]] to query the analysis manager of a project. You can run
 * the analysis afterwards as follows:
 * {{{
 * val analysisManager = project.get(FPCFAnalysisManagerKey)
 * analysisManager.run(InheritableMethodAnalysis)
 * }}}
 * For detailed information see the documentation of the analysis manager.
 *
 * The results of this analysis are stored in the property store of the project. You can receive
 * the results as follows:
 * {{{
 * val thePropertyStore = theProject.get(SourceElementsPropertyStoreKey)
 * val property = thePropertyStore(method, ClientCallableKey)
  * property match {
  *   case Some(IsClientCallable) => ...
  *   case Some(NotClientCallable) => ...
  *   case None => ... // this happens only if an not supported entity is passed to the computation.
  * }
 * }}}
 *
 * @note This analysis implements a direct property computation that is only executed when
  * 		required.
  *
 * @author Michael Reif
 */
class CallableByClientAnalysis private (
        val project: SomeProject
) extends FPCFAnalysis {

    /**
     * Determines whether a method could be inherited by a library client.
     * It should not be called if the current analysis mode is application-related.
     *
     */
    def clientCallability(e: Entity): Option[Property] = {
        if (!e.isInstanceOf[Method])
            return None;

        val method = e.asInstanceOf[Method]

        if (method.isPrivate)
            return Some(NotClientCallable);

        val classFile = project.classFile(method)

        if (classFile.isEffectivelyFinal)
            return Some(NotClientCallable);

        if (isClosedLibrary && method.isPackagePrivate)
            return Some(NotClientCallable);

        if (classFile.isPublic || isOpenLibrary)
            return Some(IsClientCallable);

        val classType = classFile.thisType
        val classHierarchy = project.classHierarchy
        val methodName = method.name
        val methodDescriptor = method.descriptor

        val subtypes = mutable.Queue.empty ++= classHierarchy.directSubtypesOf(classType)
        while (subtypes.nonEmpty) {
            val subtype = subtypes.dequeue()
            project.classFile(subtype) match {
                case Some(subclass) if subclass.isClassDeclaration || subclass.isEnumDeclaration ⇒
                    if (subclass.findMethod(methodName, methodDescriptor).isEmpty) {
                        if (subclass.isPublic)
                            // the original method is now visible (and not shadowed)
                            return Some(IsClientCallable);
                    } else
                        subtypes ++= classHierarchy.directSubtypesOf(subtype)

                // we need to continue our search for a class that makes the method visible
                case None ⇒
                    // The type hierarchy is obviously not downwards closed; i.e.,
                    // the project configuration is rather strange!
                    return Some(IsClientCallable);
                case _ ⇒
            }
        }

        Some(NotClientCallable)
    }
}

object CallableByClientAnalysis extends FPCFAnalysisRunner {

    override def derivedProperties: Set[PropertyKind] = Set(ClientCallableKey)

    protected[analysis] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new CallableByClientAnalysis(project)
        propertyStore <<! (ClientCallableKey, analysis.clientCallability)
        analysis
    }
}