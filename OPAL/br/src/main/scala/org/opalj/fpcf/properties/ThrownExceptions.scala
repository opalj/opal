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
import org.opalj.br.collection.mutable.{TypesSet ⇒ BRMutableTypesSet}
import org.opalj.br.analyses.SomeProject

/**
 * Specifies for each method the exceptions that are potentially thrown by the respective method.
 * This includes the set of exceptions thrown by called methods (if any). The property '''does not
 * take the exceptions of methods which override the respective method into account'''.
 * Nevertheless, in case of a method call all potential receiver methods are
 * taken into consideration; if the set is unbounded, `ThrownExceptionsAreUnknown` is returned.
 *
 * Note that it may be possible to compute some meaningful upper type bound for the set of
 * thrown exceptions even if methods are called for which the set of thrown exceptions is unknown.
 * This is generally the case if those calls are all done in a try block but the catch/finally
 * blocks only calls known methods - if any.
 * An example is shown next and even if we assume that we don't know
 * the exceptions potentially thrown by `Class.forName`, we could still determine that this method
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
sealed abstract class ThrownExceptions extends Property {

    final type Self = ThrownExceptions

    final def key = ThrownExceptions.Key

    /**
     * Returns `true` if and only if the method does not yet throw exceptions. I.e., if the
     * this property is still refinable then this property may still change. Otherwise,
     * the analysis was able to determine that no exceptions are thrown.
     */
    def throwsNoExceptions: Boolean
}

object ThrownExceptions {

    private[this] final val cycleResolutionStrategy = (
        ps: PropertyStore,
        epks: Iterable[SomeEPK]
    ) ⇒ {
        // IMPROVE We should have support to handle cycles of "ThrownExceptions"
        val exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)

        var unknownExceptions: String = null
        epks.foreach {
            case EPK(e, ThrownExceptionsByOverridingMethods.Key) ⇒
                ps(e, ThrownExceptionsByOverridingMethods.Key).p match {
                    case UnknownThrownExceptionsByOverridingMethods ⇒
                        unknownExceptions = "Overridden method throw unknown exceptions"
                    case c: AllThrownExceptionsByOverridingMethods ⇒
                        exceptions ++= c.exceptions.concreteTypes
                }

            case EPK(e, Key) ⇒
                ps(e, Key).p match {
                    case u: ThrownExceptionsAreUnknown ⇒ unknownExceptions = u.reason
                    case t: AllThrownExceptions        ⇒ exceptions ++= t.types.concreteTypes
                }
        }

        val p = if (unknownExceptions != null) {
            ThrownExceptionsAreUnknown(unknownExceptions)
        } else if (exceptions.nonEmpty) {
            new AllThrownExceptions(exceptions, false)
        } else {
            NoExceptionsAreThrown.NoInstructionThrowsExceptions
        }

        val e = epks.find(_.pk == Key).get.e
        Iterable(Result(e, p))
    }

    final val Key: PropertyKey[ThrownExceptions] = {
        PropertyKey.create[ThrownExceptions](
            "ThrownExceptions",
            ThrownExceptionsFallback,
            cycleResolutionStrategy
        )
    }
}

class AllThrownExceptions(
        val types:       BRTypesSet,
        val isRefinable: Boolean
) extends ThrownExceptions {

    override def throwsNoExceptions: Boolean = types.isEmpty

    override def toString: String = s"AllThrownExceptions($types)"

    override def equals(other: Any): Boolean = {
        other match {
            case that: AllThrownExceptions ⇒
                this.types == that.types && this.isRefinable == that.isRefinable
            case _ ⇒ false
        }
    }

    override def hashCode: Int = 13 * types.hashCode + (if (isRefinable) 41 else 53)

}

final case class NoExceptionsAreThrown(
        explanation: String
) extends AllThrownExceptions(BRTypesSet.empty, isRefinable = false) {

    override def throwsNoExceptions: Boolean = true

    override def toString: String = s"NoExceptionsAreThrown($explanation)"
}

object NoExceptionsAreThrown {

    final val NoInstructionThrowsExceptions = {
        NoExceptionsAreThrown("none of the instructions of the method throws an exception")
    }

    final val MethodIsAbstract = NoExceptionsAreThrown("method is abstract")

}

final case class ThrownExceptionsAreUnknown(reason: String) extends ThrownExceptions {

    override def throwsNoExceptions: Boolean = false // <= they are unknown

    def isRefinable: Boolean = false

}

object ThrownExceptionsAreUnknown {

    final val MethodIsNative = ThrownExceptionsAreUnknown("the method is native")

    final val UnresolvableCycle = {
        ThrownExceptionsAreUnknown("a cycle was detected which the analysis could not resolve")
    }

    final val UnknownExceptionIsThrown = {
        ThrownExceptionsAreUnknown("unable to determine the precise type(s) of a thrown exception")
    }

    final val UnresolvedInvokeDynamic = {
        ThrownExceptionsAreUnknown("the call targets of the unresolved invokedynamic are unknown")
    }

    final val MethodBodyIsNotAvailable = {
        ThrownExceptionsAreUnknown("the method body (of the concrete method) is not available")
    }

    final val UnboundedTargetMethods = {
        ThrownExceptionsAreUnknown("the set of target methods is unbounded/extensible")
    }

    final val AnalysisLimitation = {
        ThrownExceptionsAreUnknown(
            "the analysis is too simple to compute a sound approximation of the thrown exceptions"
        )
    }

    final val SubclassesHaveUnknownExceptions = {
        ThrownExceptionsAreUnknown("one or more subclass throw unknown exceptions")
    }

    final val MethodIsOverrideable = {
        ThrownExceptionsAreUnknown("the method is overrideable by a not yet existing type")
    }

}
