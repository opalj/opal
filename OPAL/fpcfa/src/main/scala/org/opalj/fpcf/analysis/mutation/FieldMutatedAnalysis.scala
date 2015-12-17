/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package mutation

import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.instructions.PUTSTATIC

/**
 * Determines if a field is always initialized at most once or if a field is or can be mutated
 * after (lazy) initialization.
 */
class FieldMutatedAnalysis private (
        val project: SomeProject
) extends FPCFAnalysis {

    def determineProperty(
        classFile: ClassFile
    ): PropertyComputationResult = {

        val thisType = classFile.thisType
        val fields = classFile.fields
        val psnfFields = fields.filter(f ⇒ f.isPrivate && f.isStatic && !f.isFinal).toSet
        var effectivelyFinalFields = psnfFields
        if (psnfFields.isEmpty)
            return Empty;

        val concreteMethods = classFile.methods filter { m ⇒
            !m.isStaticInitializer /*the static initializer is only executed once and at the beginning */ &&
                !m.isNative &&
                !m.isAbstract
        }
        concreteMethods foreach { m ⇒
            m.body.get foreach { (pc, instruction) ⇒
                instruction match {
                    case PUTSTATIC(`thisType`, fieldName, fieldType) ⇒
                        // we don't need to lookup the field in the
                        // class hierarchy since we are only concerned about private
                        // fields so far... so we don't have to do a full
                        // resolution of the field reference.
                        val field = classFile.findField(fieldName)
                        if (field.isDefined) { effectivelyFinalFields -= field.get }
                        if (effectivelyFinalFields.isEmpty)
                            return ImmediateMultiResult(psnfFields.map(f ⇒ (f, NonFinalByAnalysis)));
                    case _ ⇒
                    /*Nothing to do*/
                }
            }
        }

        val results = psnfFields map { f ⇒
            if (effectivelyFinalFields.contains(f))
                (f, EffectivelyFinal)
            else
                (f, NonFinalByAnalysis)
        }
        ImmediateMultiResult(results)
    }
}

object FieldMutatedAnalysis extends FPCFAnalysisRunner {

    def entitySelector(project: SomeProject): PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile if !project.isLibraryType(cf) ⇒ cf
    }

    def derivedProperties: Set[PropertyKind] = Set(Mutated)

    protected[analysis] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new FieldMutatedAnalysis(project)
        propertyStore <||< (entitySelector(project), analysis.determineProperty)
        analysis
    }
}
