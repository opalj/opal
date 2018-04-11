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
package properties

import org.opalj.br.collection.{TypesSet ⇒ BRTypesSet}

/**
 * The set of exceptions that are potentially thrown by a specific method.
 * This includes the set of exceptions thrown by (transitively) called methods (if any).
 * The property '''does not take the exceptions of methods which override the respective
 * method into account'''.
 * Nevertheless, in case of a method call all potential receiver methods are
 * taken into consideration.
 *
 * Note that it may be possible to compute some meaningful upper type bound for the set of
 * thrown exceptions even if methods are called for which the set of thrown exceptions is unknown.
 * This is generally the case if those calls are all done in a try block but the catch/finally
 * blocks only calls known methods - if any.
 * An example is shown next and even if we assume that we don't know
 * the exceptions potentially thrown by `Class.forName` we could still determine that this method
 * will never throw an exception.
 * {{{
 * object Validator {
 *      def isAvailable(s : String) : Boolean = {
 *          try { Class.forName(s); true} finally {return false;}
 *      }
 * }
 * }}}
 *
 * @author Michael Eichberg
 */
case class ThrownExceptions(types: BRTypesSet) extends Property {

    final type Self = ThrownExceptions

    final def key = ThrownExceptions.Key

    /**
     * Returns `true` if and only if the method does not yet throw exceptions. I.e., if
     * this property is still refineable then this property may still change. Otherwise,
     * the analysis was able to determine that no exceptions are thrown.
     */
    def throwsNoExceptions: Boolean = types.isEmpty

    override def toString: String = {
        if (this == ThrownExceptions.MethodIsNative)
            """ThrownExceptionsAreUnknown(reason="method is native")"""
        else if (this == ThrownExceptions.UnknownExceptionIsThrown)
            """ThrownExceptionsAreUnknown(reason="the exception type is unknown")"""
        else if (this == ThrownExceptions.MethodBodyIsNotAvailable)
            """ThrownExceptionsAreUnknown(reason="method body is not available")"""
        else if (this == ThrownExceptions.MethodIsOverrideable)
            """ThrownExceptionsAreUnknown(reason="method could be overridden by unknown method")"""
        else if (this == ThrownExceptions.AnalysisLimitation)
            """ThrownExceptionsAreUnknown(reason="analysis limitation")"""
        else
            super.toString
    }
}

object ThrownExceptions {

    private val cycleResolutionStrategy = {
        (ps: PropertyStore, eps: SomeEPS) ⇒ { Result(eps.e, eps.p) }: Result
    }

    final val Key: PropertyKey[ThrownExceptions] = {
        PropertyKey.create("ThrownExceptions", ThrownExceptionsFallback, cycleResolutionStrategy)
    }

    final val NoExceptions = new ThrownExceptions(BRTypesSet.empty)

    // in the following we use a specific instance to identify a specific reason...
    final def SomeException = new ThrownExceptions(BRTypesSet.SomeException)

    final val MethodIsNative = SomeException

    final val UnknownExceptionIsThrown = SomeException

    final val MethodBodyIsNotAvailable = SomeException

    final val MethodIsOverrideable = SomeException

    final val AnalysisLimitation = SomeException

}
