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
import org.opalj.br.ClassFile
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.FieldMutability

/**
 * Determines if a private static non-final field is always initialized at most once or
 * if a field is or can be mutated after (lazy) initialization. Field read and writes at
 * initialization time (e.g., if the current class object is registered in some publically
 * available data-store) are not considered. This is in-line with the semantics of final,
 * which also does not prevent reads of partially initialized objects.
 */
class L0FieldMutabilityAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    /**
     * Analyzes the mutability of private static non-final fields.
     *
     * This analysis is only ''defined and soundy'' if the class file does not contain native
     * methods and the method body of all non-abstract methods is available.
     * (If the analysis is scheduled using its companion object all class files with
     * native methods are filtered.)
     *
     * @param classFile A ClassFile without native methods and where the method body of all
     *                  non-abstract methods is available.
     */
    def determineFieldMutabilities(classFile: ClassFile): PropertyComputationResult = {
        val thisType = classFile.thisType
        val fields = classFile.fields
        // IMPROVE Use access flags matcher instead of querying the access flags one after another.
        val psnfFields = fields.filter(f ⇒ f.isPrivate && f.isStatic && !f.isFinal).toSet
        if (psnfFields.isEmpty)
            return NoResult;

        val finalFields = fields.collect { case f if f.isFinal ⇒ EP(f, DeclaredFinalField) }

        var effectivelyFinalFields = psnfFields
        val allMethodsIterator = classFile.methods.iterator
        val methodsIterator =
            allMethodsIterator.filter(m ⇒ !m.isStaticInitializer && !m.isAbstract)

        var continue: Boolean = true
        while (methodsIterator.hasNext && continue) {
            val m = methodsIterator.next
            val code = m.body.get
            val allFieldsNonFinal = code.exists { (pc, instruction) ⇒
                instruction match {

                    case PUTSTATIC(`thisType`, fieldName, fieldType) ⇒
                        // We don't need to lookup the field in the class
                        // hierarchy since we are only concerned about private
                        // fields so far... so we don't have to do a full
                        // resolution of the field reference.
                        val field = classFile.findField(fieldName, fieldType)
                        if (field.isDefined) {
                            effectivelyFinalFields -= field.get
                            effectivelyFinalFields.isEmpty // <=> true aborts the analysis
                        } else {
                            false
                        }

                    case _ ⇒
                        false /*Nothing to do*/
                }
            }
            continue = !allFieldsNonFinal
        }

        val psnfFieldsAnalysisResult = psnfFields map { f ⇒
            if (effectivelyFinalFields.contains(f))
                EP(f, EffectivelyFinalField)
            else
                EP(f, NonFinalFieldByAnalysis)
        }

        ImmediateMultiResult(psnfFieldsAnalysisResult ++ finalFields)
    }
}

/**
 * Factory object to create instances of the FieldMutabilityAnalysis.
 */
object L0FieldMutabilityAnalysis extends FPCFAnalysisRunner {

    def derivedProperties: Set[PropertyKind] = Set(FieldMutability)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L0FieldMutabilityAnalysis(project)
        val classFileCandidates =
            if (project.libraryClassFilesAreInterfacesOnly)
                project.allProjectClassFiles
            else
                project.allClassFiles

        val classFiles = classFileCandidates.filter(cf ⇒ cf.methods.forall(m ⇒ !m.isNative))

        propertyStore.scheduleForEntities(classFiles)(analysis.determineFieldMutabilities)
        analysis
    }
}
