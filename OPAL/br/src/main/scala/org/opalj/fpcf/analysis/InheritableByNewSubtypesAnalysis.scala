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
package org.opalj
package fpcf
package analysis

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.fpcf.properties.InheritableByNewTypes
import org.opalj.fpcf.properties.NotInheritableByNewTypes
import org.opalj.fpcf.properties.IsInheritableByNewTypes
import scala.collection.mutable

/**
 * This analysis computes the InheritableByNewTypes property.* I.e., it determines whether a method can directly be inherited by a future subtype.
 * This is in particular important when analyzing libraries.
 *
 * The analysis assumes that packages that start with "java." are closed, i.e., that no client can put a class into these specific packages.
 *
 * == Usage ==
 *
 * Use the [[FPCFAnalysesManagerKey]] to query the analysis manager of a project. You can run
 * the analysis afterwards as follows:
 * {{{
 * val analysisManager = project.get(FPCFAnalysisManagerKey)
 * analysisManager.run(InheritableByNewSubtypesAnalysis)
 * }}}
 * For detailed information see the documentation of the analysis manager.
 *
 * The results of this analysis are stored in the property store of the project. You can receive
 * the results as follows:
 * {{{
 * val thePropertyStore = theProject.get(SourceElementsPropertyStoreKey)
 * val property = thePropertyStore(method, InheritableByNewTypes.Key)
 * property match {
 *   case Some(IsInheritableByNewTypes) => ...
 *   case Some(NotInheritableByNewTypes) => ...
 *   case None => ... // this happens only if a not supported entity is passed to the computation.
 * }
 * }}}
 *
 * == Implementation ==
 *
 * This analysis computes the [[org.opalj.fpcf.properties.InheritableByNewTypes]] property.
 * Since this makes only sense when libraries are analyzed, using the application mode will
 * result in the [[org.opalj.fpcf.properties.NotInheritableByNewTypes]] for every given entity.
 *
 * This analysis considers all scenarios that are documented by the
 * [[org.opalj.fpcf.properties.InheritableByNewTypes]] property.
 *
 * @note This analysis implements a direct property computation that is only executed when
 *      required.
 * @author Michael Reif
 */
class InheritableByNewSubtypesAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    /**
     * Determines whether a method can be inherited by a library client.
     * This should not be called if the current analysis mode is application-related.
     *
     */
    def subtypeInheritability(
        isApplicationMode: Boolean
    )(
        e: Entity
    ): Property = {
        val method = e.asInstanceOf[Method]

        if (isApplicationMode)
            return NotInheritableByNewTypes;

        if (method.isPrivate)
            return NotInheritableByNewTypes;

        val classFile = project.classFile(method)
        if (classFile.isEffectivelyFinal)
            return NotInheritableByNewTypes;

        //packages that start with "java." are closed, even under the open packages assumption
        val isJavaPackage = classFile.thisType.packageName.startsWith("java.")
        if ((isClosedLibrary || isJavaPackage)
            && method.isPackagePrivate)
            return NotInheritableByNewTypes;

        if (classFile.isPublic ||
            isOpenLibrary && !isJavaPackage)
            return IsInheritableByNewTypes;

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
                            return IsInheritableByNewTypes;
                    } else
                        subtypes ++= classHierarchy.directSubtypesOf(subtype)

                // we need to continue our search for a class that makes the method visible
                case None ⇒
                    // The type hierarchy is obviously not downwards closed; i.e.,
                    // the project configuration is rather strange!
                    return IsInheritableByNewTypes;
                case _ ⇒
            }
        }

        NotInheritableByNewTypes
    }
}

object InheritableByNewSubtypesAnalysis extends FPCFAnalysisRunner {

    override def derivedProperties: Set[PropertyKind] = Set(InheritableByNewTypes.Key)

    protected[fpcf] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new InheritableByNewSubtypesAnalysis(project)
        val isApplicationMode: Boolean = AnalysisModes.isApplicationLike(project.analysisMode)
        propertyStore scheduleOnDemandComputation (
            InheritableByNewTypes.Key,
            analysis.subtypeInheritability(isApplicationMode)
        )
        analysis
    }
}
