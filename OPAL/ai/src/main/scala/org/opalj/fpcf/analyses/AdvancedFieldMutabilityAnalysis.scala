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
 * Determines if a private field is always initialized at most once or if a field is or can be
 * mutated after (lazy) initialization.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 * @author Michael Eichberg
 */
class AdvancedFieldMutabilityAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    final val tacai = project.get(DefaultTACAIKey)

    /**
     * Analyzes the mutability of private non-final fields.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * If the analysis is schedulued using its companion object all class files with
     * native methods are filtered.
     */
    def determineFieldMutabilities(classFile: ClassFile): PropertyComputationResult = {
        val thisType = classFile.thisType
        val fields = classFile.fields
        val pnfFields = fields.filter(f ⇒ f.isPrivate && !f.isFinal).toSet

        if (pnfFields.isEmpty)
            return NoResult;


// We now have to analyze the static initializer as the static initializer
// can be used to initialize a private field of an instance of the class file
// after the reference to the class file and (an indirect) reference to the
// field has become available. Consider the following example:
//    class X implementes Y{
//
//        private Object o;
//
//        public Object getO() { return o; }
//
//        private static X instance;
//        static {
//            instance = new X();
//            Z.register(instance);
//            // when we reach this point o is now (via getO) publically accessible and
//            // X is properly initialized!
//            o = new Object(); // o is mutated...
//        }
//    }

        // IMPROVE Implement special handling for those methods that are always (guaranteed) only called by the constructor. (Note: filtering is not possible as the reference to this object may leak!)

        var effectivelyFinalFields = pnfFields
        for {
            method ← classFile.methods
            if !method.isAbstract
        } {
            tacai(method).stmts exists {

                case PutStatic(_, `thisType`, fieldName, fieldType, _) ⇒
                    val field = classFile.findField(fieldName, fieldType)
                    field.foreach(effectivelyFinalFields -= _)
                    effectivelyFinalFields.isEmpty // <=> true will abort the querying of the code
                // for instance fields it should be okay if they are written in the constructor

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

        val pnfFieldsEPs = pnfFields.map { f ⇒
            if (effectivelyFinalFields.contains(f))
                EP(f, EffectivelyFinalField)
            else
                EP(f, NonFinalFieldByAnalysis)
        }

        ImmediateMultiResult(
            pnfFieldsEPs ++ fields.collect { case f if f.isFinal ⇒ EP(f, DeclaredFinalField) }
        )
    }
}

object AdvancedFieldMutabilityAnalysis extends FPCFAnalysisRunner {

    def derivedProperties: Set[PropertyKind] = Set(FieldMutability)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new AdvancedFieldMutabilityAnalysis(project)
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
