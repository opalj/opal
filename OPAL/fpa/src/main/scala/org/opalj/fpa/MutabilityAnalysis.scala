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
package fpa

import scala.language.postfixOps
import java.net.URL
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.analyses.{ Project, SomeProject }
import org.opalj.br.ClassFile
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NEWARRAY
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ARRAYLENGTH
import org.opalj.br.instructions.LALOAD
import org.opalj.br.instructions.IALOAD
import org.opalj.br.instructions.CALOAD
import org.opalj.br.instructions.BALOAD
import org.opalj.br.instructions.BASTORE
import org.opalj.br.instructions.CASTORE
import org.opalj.br.instructions.IASTORE
import org.opalj.br.instructions.LASTORE
import org.opalj.br.instructions.SASTORE
import org.opalj.br.instructions.SALOAD
import org.opalj.br.instructions.DALOAD
import org.opalj.br.instructions.FALOAD
import org.opalj.br.instructions.FASTORE
import org.opalj.br.instructions.DASTORE
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.fp.Empty
import org.opalj.fp.{ Property, PropertyComputationResult, PropertyStore, PropertyKey }
import org.opalj.fp.ImmediateMultiResult

sealed trait Mutability extends Property {
    final def key = Mutability.Key // All instances have to share the SAME key!
}

object Mutability {
    final val Key = PropertyKey.create("Mutability", NonFinal)
}

case object EffectivelyFinal extends Mutability { final val isRefineable = false }

case object NonFinal extends Mutability { final val isRefineable = false }

object MutablityAnalysis {

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineMutabilityOfNonFinalPrivateStaticFields(
        classFile: ClassFile)(
            implicit project: SomeProject,
            projectStore: PropertyStore): PropertyComputationResult = {

        val thisType = classFile.thisType
        val fields = classFile.fields
        val psnfFields = fields.filter(f ⇒ f.isPrivate && f.isStatic && !f.isFinal).toSet
        var effectivelyFinalFields = psnfFields
        if (psnfFields.isEmpty)
            return Empty;

        val concreteMethods = classFile.methods filter { m ⇒
            !m.isStaticInitializer && !m.isNative && !m.isAbstract
        }
        concreteMethods foreach { m ⇒
            m.body.get foreach { (pc, instruction) ⇒
                instruction match {
                    case PUTSTATIC(`thisType`, fieldName, fieldType) ⇒
                        // we don't need to lookup the field in the
                        // class hierarchy since we are only concerned about private
                        // fields so far... so we don't have to do a full
                        // resolution of the field reference.
                        classFile.findField(fieldName) foreach { f ⇒ effectivelyFinalFields -= f }
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

    def analyze(implicit project: Project[URL]): Unit = {
        implicit val projectStore = project.get(SourceElementsPropertyStoreKey)
        projectStore <||< (
            { case cf: ClassFile ⇒ cf },
            determineMutabilityOfNonFinalPrivateStaticFields)
    }
}

