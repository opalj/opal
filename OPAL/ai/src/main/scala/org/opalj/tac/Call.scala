/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
     * The parameters of the method call (excluding the implicit `this` reference.)
     */
    def params: Seq[Expr[V]] // TODO IndexedSeq

    /**
     * The parameters of the method call (including the implicit `this` reference if necessary.)
     */
    def allParams: Seq[Expr[V]]
}

object Call {

    def unapply[V <: Var[V]](
        call: Call[V]
    ): Some[(ReferenceType, Boolean, String, MethodDescriptor)] = {
        Some((call.declaringClass, call.isInterface, call.name, call.descriptor))
    }
}

