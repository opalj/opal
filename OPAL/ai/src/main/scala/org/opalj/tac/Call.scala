/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import scala.collection.Set

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectLike
import org.opalj.value.ValueInformation

/**
 * Common supertrait of statements and expressions calling a method.
 *
 * @author Michael Eichberg
 */
trait Call[+V <: Var[V]] {

    /** The declaring class; can be an array type for all methods defined by `java.lang.Object`. */
    def declaringClass: ReferenceType
    /** `true` iff the declaring class is an interface. */
    def isInterface: Boolean
    def name: String
    def descriptor: MethodDescriptor

    /**
     * The parameters of the call (excluding the implicit `this` reference.)
     */
    def params: Seq[Expr[V]] // TODO IndexedSeq

    /**
     * The parameters of the call (including the implicit `this` reference if necessary.)
     */
    def allParams: Seq[Expr[V]]

    /**
     * The receiver of the call if it is an instance call, None if it is a static call.
     */
    def receiverOption: Option[Expr[V]]

    /**
     * Convenience method which abstracts over all kinds of calls; not all information is
     * always required.
     */
    def resolveCallTargets(
        callingContext: ObjectType
    )(
        implicit
        p:  ProjectLike,
        ev: V <:< DUVar[ValueInformation]
    ): Set[Method]
}

object Call {

    def unapply[V <: Var[V]](
        call: Call[V]
    ): Some[(ReferenceType, Boolean, String, MethodDescriptor)] = {
        Some((call.declaringClass, call.isInterface, call.name, call.descriptor))
    }
}

