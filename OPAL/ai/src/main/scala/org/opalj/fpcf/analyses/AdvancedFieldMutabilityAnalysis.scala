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

import org.opalj.br.ClassFile
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntArraySet
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.PutStatic
import org.opalj.tac.PutField
import org.opalj.tac.UVar

/**
 * Determines if a field is always initialized at most once or if a field is or can be
 * mutated after (lazy) initialization.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 * @note Requires flat hierarchy of the three address code.
 */
class AdvancedFieldMutabilityAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    def determineFieldMutabilities(classFile: ClassFile): PropertyComputationResult = {
        val thisType = classFile.thisType
        val fields = classFile.fields
        val pnfFields = fields.filter(f ⇒ f.isPrivate && !f.isFinal).toSet
        var effectivelyFinalFields = pnfFields
        if (effectivelyFinalFields.isEmpty) return NoResult
        for {
            method ← classFile.methods
            if !method.isStaticInitializer
            if !method.isAbstract
            if !method.isNative
        } {
            val code = project.get(DefaultTACAIKey)(method).stmts
            code exists {
                // static fields should only be written in the static initializer
                case PutStatic(_, `thisType`, fieldName, fieldType, _) ⇒
                    val field = classFile.findField(fieldName, fieldType)
                    field.foreach(effectivelyFinalFields -= _)
                    effectivelyFinalFields.isEmpty // <=> true will abort the querying of the code
                // for instance fields it should be okay if they where written in the constructor
                case PutField(_, `thisType`, fieldName, fieldType, objRef, _) ⇒
                    val field = classFile.findField(fieldName, fieldType)
                    field foreach { f ⇒
                        if (method.isConstructor) {
                            // note that here we assume real three address code (flat hierarchy)
                            val UVar(_, defSites) = objRef
                            // if the field that is written is not the one of the this local
                            // it is not effectively final
                            if (defSites != IntArraySet(-1)) {
                                effectivelyFinalFields -= f
                            }

                        } else {
                            effectivelyFinalFields -= f
                        }
                    }

                    effectivelyFinalFields.isEmpty // <=> true will abort the querying of the code
                case _ ⇒ false
            }
        }

        val pnfFieldsResult = pnfFields map { f ⇒
            if (effectivelyFinalFields.contains(f))
                EP(f, EffectivelyFinalField)
            else
                EP(f, NonFinalFieldByAnalysis)
        }

        val r = pnfFieldsResult ++ fields.collect { case f if f.isFinal ⇒ EP(f, DeclaredFinalField) }
        ImmediateMultiResult(r)
    }
}

object AdvancedFieldMutabilityAnalysis extends FPCFAnalysisRunner {

    def derivedProperties: Set[PropertyKind] = Set(FieldMutability)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new AdvancedFieldMutabilityAnalysis(project)
        propertyStore.scheduleForCollected {
            case cf: ClassFile if (
                !project.libraryClassFilesAreInterfacesOnly || !project.isLibraryType(cf)
            ) ⇒
                cf
        }(
            analysis.determineFieldMutabilities
        )
        analysis
    }
}
