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
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.Field
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.NonFinalFieldByLackOfInformation

/**
 * Determines if a private, static, non-final field is always initialized at most once or
 * if a field is or can be mutated after (lazy) initialization. Field read and writes at
 * initialization time (e.g., if the current class object is registered in some publically
 * available data-store) are not considered. This is in-line with the semantics of final,
 * which also does not prevent reads of partially initialized objects.
 */
class L0FieldMutabilityAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final val fieldAccessInformation = project.get(FieldAccessInformationKey)

    /**
     * Invoked for in the lazy computation case.
     * Final fields are considered [[org.opalj.fpcf.properties.DeclaredFinalField]], non-final and
     * non-private fields or fields of library classes whose method bodies are not available are
     * considered [[org.opalj.fpcf.properties.NonFinalFieldByAnalysis]].
     * For all other cases the call is delegated to [[determineFieldMutability]].
     */
    def determineFieldMutabilityLazy(e: Entity): PropertyComputationResult = {
        e match {
            case field: Field ⇒
                determineFieldMutability(field)

            case _ ⇒
                val m = e.getClass.getSimpleName+" is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    /**
     * Analyzes the mutability of private static non-final fields.
     *
     * This analysis is only ''defined and soundy'' if the class file does not contain native
     * methods and the method body of all non-abstract methods is available.
     * (If the analysis is scheduled using its companion object all class files with
     * native methods are filtered.)
     *
     * @param field A field without native methods and where the method body of all
     *                  non-abstract methods is available.
     */
    def determineFieldMutability(field: Field): PropertyComputationResult = {
        if (field.isFinal)
            return Result(field, DeclaredFinalField)

        if (!field.isPrivate)
            return Result(field, NonFinalFieldByLackOfInformation)

        if (!field.isStatic)
            return Result(field, NonFinalFieldByLackOfInformation)

        if (field.classFile.methods.exists(_.isNative))
            return Result(field, NonFinalFieldByLackOfInformation)

        val classFile = field.classFile
        val thisType = classFile.thisType

        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            if !method.isStaticInitializer
            pc ← pcs
        } {
            method.body.get.instructions(pc) match {
                case PUTSTATIC(`thisType`, fieldName, fieldType) ⇒
                    // We don't need to lookup the field in the class
                    // hierarchy since we are only concerned about private
                    // fields so far... so we don't have to do a full
                    // resolution of the field reference.
                    val field = classFile.findField(fieldName, fieldType)
                    if (field.isDefined) {
                        return Result(field.get, NonFinalFieldByAnalysis);
                    }

                case _ ⇒
            }
        }

        Result(field, EffectivelyFinalField)
    }
}

trait L0FieldMutabilityAnalysisScheduler extends ComputationSpecification {
    def uses: Set[PropertyKind] = Set.empty

    def derives: Set[PropertyKind] = Set(FieldMutability)
}

/**
 * Factory object to create instances of the FieldMutabilityAnalysis.
 */
object EagerL0FieldMutabilityAnalysis
    extends L0FieldMutabilityAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L0FieldMutabilityAnalysis(project)
        val classFileCandidates =
            if (project.libraryClassFilesAreInterfacesOnly)
                project.allProjectClassFiles
            else
                project.allClassFiles

        val fields = {
            classFileCandidates.filter(cf ⇒ cf.methods.forall(m ⇒ !m.isNative)).flatMap(_.fields)
        }

        propertyStore.scheduleForEntities(fields)(analysis.determineFieldMutability)
        analysis
    }
}

object LazyL0FieldMutabilityAnalysis
    extends L0FieldMutabilityAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L0FieldMutabilityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            FieldMutability.key,
            (field: Field) ⇒ analysis.determineFieldMutabilityLazy(field)
        )
        analysis
    }

}
