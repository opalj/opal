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
package br
package instructions

import org.opalj.collection.immutable.Chain

/**
 * Set field in object.
 *
 * @see [[org.opalj.br.instructions.FieldAccess]] for additional
 *      pattern matching support.
 *
 * @author Michael Eichberg
 */
case class PUTFIELD(
        declaringClass: ObjectType,
        name:           String,
        fieldType:      FieldType
) extends FieldWriteAccess {

    final def opcode: Opcode = PUTFIELD.opcode

    final def mnemonic: String = "putfield"

    final def jvmExceptions: List[ObjectType] = FieldAccess.jvmExceptions

    final def mayThrowExceptions: Boolean = true

    final def numberOfPoppedOperands(ctg: Int ⇒ ComputationalTypeCategory): Int = 2

    final def stackSlotsChange: Int = -fieldType.computationalType.operandSize - 1

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Chain[PC] = {
        if (regularSuccessorsOnly)
            Chain.singleton(indexOfNextInstruction(currentPC))
        else
            Instruction.nextInstructionOrExceptionHandler(
                this, currentPC, ObjectType.NullPointerException
            )
    }

    override def toString = "put "+declaringClass.toJava+"."+name+" : "+fieldType.toJava

}

/**
 * General information and factory methods.
 *
 * @author Malte Limmeroth
 */
object PUTFIELD {

    final val opcode = 181

    /**
     * Factory method to create [[PUTFIELD]] instructions.
     *
     * @param   declaringClassName The binary name of the field's declaring class, e.g.
     *          `java/lang/Object`.
     * @param   name The field's name.
     * @param   fieldTypeName The field's type; see [[org.opalj.br.FieldType$]] for the concrete
     *          syntax.
     */
    def apply(declaringClassName: String, name: String, fieldTypeName: String): PUTFIELD = {
        PUTFIELD(ObjectType(declaringClassName), name, FieldType(fieldTypeName))
    }

}
