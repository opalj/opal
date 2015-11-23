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

import scala.language.postfixOps
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.instructions.PUTSTATIC

class MutabilityAnalysis private (
        project:        SomeProject,
        entitySelector: PartialFunction[Entity, ClassFile]
) extends AbstractFPCFAnalysis(project, entitySelector) {

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
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
                !m.isNative && !m.isAbstract
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
                            return ImmediateMultiResult(psnfFields.map(f ⇒ (f, NonFinal)));
                    case _ ⇒
                    /*Nothing to do*/
                }
            }
        }

        val results = psnfFields map { f ⇒
            if (effectivelyFinalFields.contains(f))
                (f, EffectivelyFinal)
            else
                (f, NonFinal)
        }
        ImmediateMultiResult(results)
    }
}

object MutabilityAnalysis extends FPCFAnalysisRunner {

    def entitySelector: PartialFunction[Entity, ClassFile] = FPCFAnalysisRunner.ClassFileSelector

    def derivedProperties: Set[PropertyKind] = Set(Mutability)

    protected[analysis] def start(project: SomeProject): Unit = {
        new MutabilityAnalysis(project, entitySelector)
    }

}
