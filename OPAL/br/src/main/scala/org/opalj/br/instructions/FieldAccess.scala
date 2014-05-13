/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

/**
 * Instructions that access a class' field.
 *
 * @author Michael Eichberg
 */
sealed abstract class FieldAccess extends Instruction {

    def declaringClass: ObjectType

    def name: String

    def fieldType: FieldType

    def asVirtualField: VirtualField = VirtualField(declaringClass, name, fieldType)

    final override def indexOfNextInstruction(currentPC: Int, code: Code): Int =
        currentPC + 3

}

/**
 * Defines an extractor to facilitate pattern matching on field access instructions.
 */
object FieldAccess {

    val runtimeExceptions = List(ObjectType.NullPointerException)

    def unapply(fa: FieldAccess): Option[(ObjectType, String, FieldType)] =
        Some((fa.declaringClass, fa.name, fa.fieldType))
}

abstract class FieldReadAccess extends FieldAccess

/**
 * Defines an extractor to facilitate pattern matching on field read access instructions.
 */
object FieldReadAccess {

    def unapply(fa: FieldReadAccess): Option[(ObjectType, String, FieldType)] =
        FieldAccess.unapply(fa)
}

abstract class FieldWriteAccess extends FieldAccess

/**
 * Defines an extractor to facilitate pattern matching on field write access instructions.
 */
object FieldWriteAccess {

    def unapply(fa: FieldWriteAccess): Option[(ObjectType, String, FieldType)] =
        FieldAccess.unapply(fa)
}