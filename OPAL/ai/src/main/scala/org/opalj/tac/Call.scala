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
package tac

import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType

/**
 * Common supertrait of statements and expressions calling a method.
 *
 * @author Michael Eichberg
 */
trait Call[+V <: Var[V]] {
    /** The declaring class; can be an array type for all methods defined by `java.lang.Object`. */
    def declaringClass: ReferenceType
    def isInterface: Boolean
    def name: String
    def descriptor: MethodDescriptor

    /**
     * The parameters of the method call (including the implicit `this` reference if necessary.)
     */
    def params: Seq[Expr[V]] // TODO IndexedSeq
}

object Call {

    def unapply[V <: Var[V]](
        call: Call[V]
    ): Some[(ReferenceType, Boolean, String, MethodDescriptor)] = {
        Some((call.declaringClass, call.isInterface, call.name, call.descriptor))
    }
}


object MethodCallParameters {

    def unapply[V <: Var[V]](astNode: ASTNode[V]): Option[Seq[Expr[V]]] = {
        astNode match {
            case c: Call[V @unchecked]                   ⇒ Some(c.params)
            case Assignment(_, _, c: Call[V @unchecked]) ⇒ Some(c.params)
            case _                                       ⇒ None
        }
    }

}
