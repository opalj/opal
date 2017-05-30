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

/**
 * Part of the Java 6 stack map table attribute.
 *
 * @author Michael Eichberg
 */
sealed abstract class VerificationTypeInfo {

    def tag: Int

    def isObjectVariableInfo: Boolean = false
    def asObjectVariableInfo: ObjectVariableInfo = {
        throw new ClassCastException(s"$this cannot be cast to ObjectVariableInfo")
    }
}

case object TopVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 0
}

case object IntegerVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 1
}

case object FloatVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 2
}

case object DoubleVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 3
}

case object LongVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 4
}

case object NullVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 5
}

case object UninitializedThisVariableInfo extends VerificationTypeInfo {
    final val tag: Int = 6
}

case class ObjectVariableInfo(clazz: ReferenceType) extends VerificationTypeInfo {
    final val tag: Int = 7

    override def isObjectVariableInfo: Boolean = true
    override def asObjectVariableInfo: this.type = this
}

case class UninitializedVariableInfo(offset: Int) extends VerificationTypeInfo {
    final val tag: Int = 8
}
