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
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.PutStatic
import org.opalj.tac.PutField
import org.opalj.tac.UVar
import org.opalj.tac.SelfReferenceParameter

/**
 * Simple analysis that checks if a private (static or instance) field is always initialized at
 * most once or if a field is or can be mutated after (lazy) initialization.
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
    private def determineFieldMutabilities(classFile: ClassFile): PropertyComputationResult = {
        val thisType = classFile.thisType
        val fields = classFile.fields
        val pnfFields = fields.filter(f ⇒ f.isPrivate && !f.isFinal)

        if (pnfFields.isEmpty)
            return NoResult;

        // We now (compared to the simple one) have to analyze the static initializer as
        // the static initializer can be used to initialize a private field of an instance
        // of the class after the reference to the class and (an indirect) reference to the
        // field has become available. Consider the following example:
        // class X implements Y{
        //
        //     private Object o;
        //
        //     public Object getO() { return o; }
        //
        //     private static X instance;
        //     static {
        //         instance = new X();
        //         Z.register(instance);
        //         // when we reach this point o is now (via getO) publically accessible and
        //         // X is properly initialized!
        //         o = new Object(); // o is mutated...
        //     }
        // }

        // IMPROVE Implement special handling for those methods that are always (guaranteed) only called by the constructor. (Note: filtering is not possible as the reference to this object may leak!)

        var effectivelyFinalFields = pnfFields.toSet
        val methodsIterator = classFile.methods.iterator.filter(m ⇒ !m.isAbstract)
        // Note: we do not want to force the creation of the three address code for methods,
        // we are no longer interested in.
        while (methodsIterator.hasNext && effectivelyFinalFields.nonEmpty) {
            val method = methodsIterator.next()

            tacai(method).stmts exists {
                case PutStatic(_, `thisType`, fieldName, fieldType, _) if (
                    !method.isStaticInitializer
                ) ⇒
                    val field = classFile.findField(fieldName, fieldType)
                    field.foreach(effectivelyFinalFields -= _)
                    effectivelyFinalFields.isEmpty // <=> true will abort the querying of the code

                case PutField(_, `thisType`, fieldName, fieldType, objRef, _) ⇒
                    val fieldOption = classFile.findField(fieldName, fieldType)
                    fieldOption foreach { f ⇒
                        // for instance fields it is okay if they are written in the constructor
                        // (w.r.t. the currently initialized object!)
                        if (method.isConstructor) {
                            // note that here we assume real three address code (flat hierarchy)
                            val UVar(_, receiver) = objRef
                            // If the field that is written is not the one referred to by the
                            // self reference, it is not effectively final.
                            if (receiver != SelfReferenceParameter) {
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

/**
 * Executor for the field mutability analysis.
 */
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
