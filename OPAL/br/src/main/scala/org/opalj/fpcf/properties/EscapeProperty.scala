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

import org.opalj.fpcf.PropertyKey.SomeEPKs

import scala.annotation.switch

sealed trait EscapePropertyMetaInformation extends PropertyMetaInformation {

    final type Self = EscapeProperty
}

/**
 * Specifies the lifetime of object instance. This is classically used for compiler optimizations
 * such as scalar replacement, stack allocation or removal of synchronization.
 * However, other usages such as finding bugs, identifying pure methods or helping to
 * identify immutable data-structures are also supported.
 *
 * Choi et al. [1] describe two predicates that can be used to describe the properties relevant
 * to escape information.
 *
 * "Let O be an object instance and M be a method invocation. O is said to escape M, denoted as
 * Escapes(O, M), if the lifetime of O may exceed the lifetime of M."
 *
 * "Let O be an object instance and T be a thread (instance). O is said to escape T, again
 * denoted as Escapes(O, T), if another thread, T’ != T, may access O."
 *
 * Furthermore it holds that "For any object O, !Escapes(O, M) implies !Escapes(O, T), where method
 * M is invoked in thread T." [1]
 *
 * In contrast to this, Kotzmann and Mössenböck [2] describe the escape of an object with the access
 * to this object from other methods or threads.
 * This EscapeProperty combines both concepts and furthermore tries to be more specific about the
 * reason why an object escapes.
 *
 * In the following we provide further details about the different escape properties:
 *
 * [[NoEscape]] refers to the property of an object instance O created in method M for that
 * !Escapes(O, M) holds and no other method than M has access to O. This implies that there is no
 * method M' != M that can access O (at least when disregarding reflection and native code).
 * Objects with this property can be allocated at the stack or even scalar-replaced [2].
 *
 * An object instance O created in method M and thread T has the property [[EscapeInCallee]], if it
 * holds !Escapes(O, M) but M passes O as a parameter to a method which does not let O escape any
 * further then [[EscapeInCallee]]. This implies that only M and methods M' that are (transitively)
 * called by M have access to O.
 * For objects that have the property [[EscapeInCallee]] no synchronization is needed and they can
 * be allocated on the stack.
 *
 * For objects O, created in method M and thread T, whose lifetime exceeds its method of creation M
 * and (therefore) accessible by other methods, we provide seven different properties. For all of
 * them we assume that O M and all methods called by M do not let O escape T. But it is not
 * guarantied that O will not escape T via a caller of M.
 * The properties differ in the reason why the lifetime of O exceeds the lifetime of M.
 * [[EscapeViaReturn]] describes the case that, O is returned by M. If O has an exception type and
 * is thrown in M, it has the property [[EscapeViaAbnormalReturn]].
 * For both of them it has no consequences if O escapes T via a caller of M. This is, because
 * the execution ends with the (abnormal) return of O. All synchronization mechanisms inside of M
 * or callees of M can be removed.
 * The property [[EscapeViaParameter]] describes objects that gets assigned to a parameter of its
 * method of creation (M). If O gets assigned to p.f for a parameter p of M, it could be the case
 * that the actual parameter of p already escaped T. In this case O would also escape T directly
 * via this assignment. Therefore no synchronization for O can be removed.
 * As it could be also the case that O gets assigned to a parameter and returned by M, there are
 * also properties representing the combinations of this kind of escapes. They are
 * [[EscapeViaParameterAndAbnormalReturn]], [[EscapeViaParameterAndReturn]],
 * [[EscapeViaNormalAndAbnormalReturn]] and [[EscapeViaParameterAndNormalAndAbnormalReturn]].
 *
 *
 * An object instance O created in method M and thread T has the property [[GlobalEscape]], if it
 * holds that Escapes(O, M) and Escapes(O, T). For example this is the case if O gets assigned to
 * a static field ([[EscapeViaStaticField]] but also if assigned to a field of an
 * object that has also [[GlobalEscape]] as property ([[EscapeViaHeapObject]]).
 * Objects that have the property  [[GlobalEscape]] have to be allocated on the heap and
 * synchronization mechanisms can not be removed/proper synchronization is required if the
 * object is accessed concurrently – the latter may be the goal of static analyses that find
 * concurrency bugs). If the reason for the global escape is unspecified the case class
 * [[GlobalEscape]] is used.
 *
 * The property values are partially ordered and form a lattice. The binary relation of the order is
 * called `lessOrEqualRestrictive` and describes the restrictiveness of the scope in, which objects
 * exists. The most restrictive (top) value is [[NoEscape]] and the least restrictive (bottom) one
 * is [[GlobalEscape]].
 * A dot graph of the lattice can be found under br/src/main/resources/org/opalj/fpcf/properties.
 *
 * Algorithms are free to over approximate this property, i.e. for object
 * instance O with actual property P it is okay to say O has property P' if P > P' (or in other
 * words, P' is less restrictive than P).
 *
 * If they simply don't know the actual property they should use [[MaybeNoEscape]].
 * If we know that the actual property is at most [[EscapeInCallee]] (i.e. not [[NoEscape]],
 * [[MaybeEscapeInCallee]] should be used.
 * The same holds for every other non-bottom property.
 * E.g. [[MaybeEscapeViaParameter]] should be used if we know that the actual property is at most
 * [[EscapeViaParameter]] (i.e. neither [[NoEscape]] nor [[EscapeInCallee]].
 *
 *
 * [[org.opalj.br.AllocationSite]] and [[org.opalj.br.analyses.FormalParameter]] are generally
 * used as [[Entity]] in combination with this property.
 *
 * [1] Choi, Jong-Deok, Manish Gupta, Mauricio Serrano, Vugranam C. Sreedhar, and Sam Midkiff.
 * "Escape Analysis for Java." In Proceedings of the 14th ACM SIGPLAN Conference on
 * Object-Oriented Programming, Systems, Languages, and Applications, 1–19. OOPSLA ’99.  New
 * York, NY, USA: ACM, 1999.
 *
 * [2] Kotzmann, Thomas, and Hanspeter Mössenböck. “Escape Analysis in the Context of Dynamic
 * Compilation and Deoptimization.” In Proceedings of the 1st ACM/USENIX International Conference
 * on Virtual Execution Environments, 111–120. VEE ’05. New York, NY, USA: ACM, 2005.
 *
 * @author Florian Kuebler
 */
sealed abstract class EscapeProperty extends OrderedProperty with ExplicitlyNamedProperty with EscapePropertyMetaInformation {

    final def key: PropertyKey[EscapeProperty] = EscapeProperty.key

    def isValidSuccessorOf(old: OrderedProperty): Option[String] = {
        old match {
            case oldP: EscapeProperty ⇒
                if ((this lessOrEqualRestrictive oldP) || (this lessOrEqualRestrictive oldP.atMost))
                    None
                else
                    Some(s"non-monotonic refinement from $oldP to $this")
            case p ⇒ Some(s"illegal refinement of escape property $p to $this")
        }
    }

    /**
     * A unique id for every escape property. Used for table switches.
     */
    def propertyValueID: Int

    /**
     * Tests if this property describes equal or less restricted escapes than the given property.
     * E.g., returns `true` if this property identifies values which [[GlobalEscape]] and the given
     * property (`that`) refers to values that [[NoEscape]].
     *
     * @see [[EscapeProperty]] for further details.
     */

    def lessOrEqualRestrictive(that: EscapeProperty): Boolean

    /**
     * Computes the greatest lower bound of this and that property values.
     *
     * @param that the other escape property value.
     * @return the most restrictive escape that is less or equal restrictive than `this` and `that`.
     * @see [[EscapeProperty.lessOrEqualRestrictive]]
     */
    def meet(that: EscapeProperty): EscapeProperty = (that.propertyValueID: @switch) match {
        case NoEscape.PID             ⇒ this
        case GlobalEscape.PID         ⇒ GlobalEscape
        case EscapeViaHeapObject.PID  ⇒ EscapeViaHeapObject
        case EscapeViaStaticField.PID ⇒ EscapeViaStaticField
        case _                        ⇒ EscapeProperty(flags | that.flags)
    }

    def isBottom: Boolean

    def isTop: Boolean

    def atMost: EscapeProperty

    protected val flags: Int

}

object EscapeProperty extends EscapePropertyMetaInformation {

    def cycleResolutionStrategy(ps: PropertyStore, epks: SomeEPKs): Iterable[PropertyComputationResult] = {
        Iterable(
            Result(
                epks.head.e,
                epks.foldLeft(MaybeNoEscape: EscapeProperty) {
                    (escapeState, epk) ⇒
                        epk match {
                            case EPK(e, `key`) ⇒
                                ps(e, key).p meet escapeState
                            case _ ⇒ MaybeNoEscape
                        }
                }
            )
        )
    }

    final val key: PropertyKey[EscapeProperty] = PropertyKey.create(
        // Name of the property
        "EscapeProperty",
        // fallback value
        MaybeNoEscape,
        // cycle-resolution strategy
        cycleResolutionStrategy _
    )

    final val EMPTY_FLAGS = 0x0
    final val IN_CALLEE = 0x1
    final val VIA_PARAMETER = 0x2
    final val VIA_ABNORMAL_RETURN = 0x4
    final val VIA_RETURN = 0x8
    final val GLOBAL = 0x10
    final val MAYBE = 0x20

    def apply(flags: Int): EscapeProperty = {
        (flags: @switch) match {
            case NoEscape.flags                                          ⇒ NoEscape
            case EscapeInCallee.flags                                    ⇒ EscapeInCallee
            case EscapeViaParameter.flags                                ⇒ EscapeViaParameter
            case EscapeViaAbnormalReturn.flags                           ⇒ EscapeViaAbnormalReturn
            case EscapeViaReturn.flags                                   ⇒ EscapeViaReturn
            case EscapeViaParameterAndAbnormalReturn.flags               ⇒ EscapeViaParameterAndAbnormalReturn
            case EscapeViaParameterAndReturn.flags                       ⇒ EscapeViaParameterAndReturn
            case EscapeViaNormalAndAbnormalReturn.flags                  ⇒ EscapeViaNormalAndAbnormalReturn
            case EscapeViaParameterAndNormalAndAbnormalReturn.flags      ⇒ EscapeViaParameterAndNormalAndAbnormalReturn
            case MaybeNoEscape.flags                                     ⇒ MaybeNoEscape
            case MaybeEscapeInCallee.flags                               ⇒ MaybeEscapeInCallee
            case MaybeEscapeViaParameter.flags                           ⇒ MaybeEscapeViaParameter
            case MaybeEscapeViaAbnormalReturn.flags                      ⇒ MaybeEscapeViaAbnormalReturn
            case MaybeEscapeViaReturn.flags                              ⇒ MaybeEscapeViaReturn
            case MaybeEscapeViaParameterAndAbnormalReturn.flags          ⇒ MaybeEscapeViaParameterAndAbnormalReturn
            case MaybeEscapeViaParameterAndReturn.flags                  ⇒ MaybeEscapeViaParameterAndReturn
            case MaybeEscapeViaNormalAndAbnormalReturn.flags             ⇒ MaybeEscapeViaNormalAndAbnormalReturn
            case MaybeEscapeViaParameterAndNormalAndAbnormalReturn.flags ⇒ MaybeEscapeViaParameterAndNormalAndAbnormalReturn
            case GlobalEscape.flags                                      ⇒ GlobalEscape

        }
    }
}

/**
 * The object is accessible only from within the method of creation. Objects with this
 * escape level are also referred to as being method-local.
 *
 * @see [[EscapeProperty]] for further details.
 */
case object NoEscape extends EscapeProperty {

    final val isRefineable = false

    final val PID = 0

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "No"

    override def meet(that: EscapeProperty): EscapeProperty = that

    override final def lessOrEqualRestrictive(that: EscapeProperty): Boolean = PID == that.propertyValueID

    override def isBottom: Boolean = false

    override def isTop: Boolean = true

    override def atMost: EscapeProperty = this

    final val flags = EscapeProperty.EMPTY_FLAGS
}

/**
 * The object escapes the current method M via the arguments of a method M' that is called by M
 * but does not let the argument escape. This implies that the object is also local to the thread.
 *
 * @example
 * Given the following code:
 * {{{
 * public class X{
 *  public Object f;
 *  public void foo() {
 *   Object o = new Object();        // ALLOCATION SITE
 *   bar(o);
 *  }
 *  public int bar(Object p) {
 *   if (p == null) // do not let p escape
 *    return -1;
 *   return 0;
 *  }
 * }
 * }}}
 * An analysis is only expected to return [[EscapeInCallee]] for the object o
 * instantiated in foo, if the analyses knows(!) that no subclass of X overrides bar s.t. it let
 * its parameter escape.
 * @see [[EscapeProperty]] for further details.
 * @author Florian Kuebler
 */
case object EscapeInCallee extends EscapeProperty {
    final val isRefineable = false

    final val PID = 1

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "InCallee"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        that.propertyValueID == PID || that.propertyValueID == NoEscape.PID

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = this

    final val flags = EscapeProperty.IN_CALLEE
}

/**
 * Characterizes escapes via an assignment to a field of a method parameter, where no caller let
 * this field escape globally. (It may additionally escape by other means too, but this property
 * was derived first. It must not be the case that an additional escape has the
 * property [[GlobalEscape]].)
 *
 * @example
 * Given the following code:
 * {{{
 * public class X{
 *  public Object f;
 *  private void foo(X param) {
 *   param.f = new Object();        // ALLOCATION SITE
 *  }
 *  public void bar() {
 *   foo(new X());
 *  }
 * }
 * }}}
 * An analysis is only expected to return [[EscapeViaParameter]] for the object o
 * instantiated in foo, if the analyses knows(!) that foo is called only from bar.
 */
case object EscapeViaParameter extends EscapeProperty {
    final val isRefineable = false

    final val PID = 2

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaParameter"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID       ⇒ true
            case EscapeInCallee.PID ⇒ true
            case PID                ⇒ true
            case _                  ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = this

    final val flags = EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER
}

/**
 * Characterizes escapes via a return statement, where no caller let this return value escape
 * globally. (It may additionally escape by other means too, but this property
 * was derived first. It must not be the case that an additional escape has the
 * property [[GlobalEscape]].)
 *
 * @note For escape characterization, a 'throw' statements is seen as a 'return' statement.
 * @example
 * Given the following code:
 * {{{
 * public class X{
 *  public Object f;
 *  private Object foo() {
 *   Object o = new Object();        // ALLOCATION SITE
 *   return o;
 *  }
 *  public void bar() {
 *   foo(); // do not use the return
 *  }
 * }
 * }}}
 * An analysis is only expected to return [[EscapeViaReturn]] for the object o
 * instantiated in foo, if the analyses knows(!) that foo is called only from bar.
 */
case object EscapeViaReturn extends EscapeProperty {
    final val isRefineable = false

    final val PID = 3

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID       ⇒ true
            case EscapeInCallee.PID ⇒ true
            case PID                ⇒ true
            case _                  ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = this

    final val flags = EscapeProperty.IN_CALLEE | EscapeProperty.VIA_RETURN
}

case object EscapeViaAbnormalReturn extends EscapeProperty {
    final val isRefineable = false

    final val PID = 4

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaAbnormalReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID       ⇒ true
            case EscapeInCallee.PID ⇒ true
            case PID                ⇒ true
            case _                  ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = this

    final val flags = EscapeProperty.IN_CALLEE | EscapeProperty.VIA_ABNORMAL_RETURN
}

case object EscapeViaParameterAndReturn extends EscapeProperty {
    final val isRefineable = false

    final val PID = 5

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaParameterAndReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID           ⇒ true
            case EscapeInCallee.PID     ⇒ true
            case EscapeViaParameter.PID ⇒ true
            case EscapeViaReturn.PID    ⇒ true
            case PID                    ⇒ true
            case _                      ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = this

    final val flags =
        EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER | EscapeProperty.VIA_RETURN
}

case object EscapeViaParameterAndAbnormalReturn extends EscapeProperty {
    final val isRefineable = false

    final val PID = 6

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaParameterAndAbnormalReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID                ⇒ true
            case EscapeInCallee.PID          ⇒ true
            case EscapeViaParameter.PID      ⇒ true
            case EscapeViaAbnormalReturn.PID ⇒ true
            case PID                         ⇒ true
            case _                           ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = this

    final val flags =
        EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER | EscapeProperty.VIA_ABNORMAL_RETURN
}

case object EscapeViaNormalAndAbnormalReturn extends EscapeProperty {
    final val isRefineable = false

    final val PID = 7

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaNormalAndAbnormalReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID                ⇒ true
            case EscapeInCallee.PID          ⇒ true
            case EscapeViaAbnormalReturn.PID ⇒ true
            case EscapeViaReturn.PID         ⇒ true
            case PID                         ⇒ true
            case _                           ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = this

    final val flags = EscapeProperty.IN_CALLEE | EscapeProperty.VIA_ABNORMAL_RETURN | EscapeProperty.VIA_RETURN
}

case object EscapeViaParameterAndNormalAndAbnormalReturn extends EscapeProperty {
    final val isRefineable = false

    final val PID = 8

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaParameterAndNormalAndAbnormalReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID                            ⇒ true
            case EscapeInCallee.PID                      ⇒ true
            case EscapeViaParameter.PID                  ⇒ true
            case EscapeViaReturn.PID                     ⇒ true
            case EscapeViaAbnormalReturn.PID             ⇒ true
            case EscapeViaNormalAndAbnormalReturn.PID    ⇒ true
            case EscapeViaParameterAndReturn.PID         ⇒ true
            case EscapeViaParameterAndAbnormalReturn.PID ⇒ true
            case PID                                     ⇒ true
            case _                                       ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = this

    final val flags = EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER | EscapeProperty.VIA_RETURN | EscapeProperty.VIA_ABNORMAL_RETURN
}

/**
 * Used, when we know nothing about the escape property so far.
 *
 * @see [[EscapeProperty]] for further details.
 * @author Florian Kuebler
 */
case object MaybeNoEscape extends EscapeProperty {

    final val isRefineable = true

    final val PID = 9

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "MaybeNo"

    override final def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        that.propertyValueID == PID || that.propertyValueID == NoEscape.PID

    override def isBottom: Boolean = false

    override def isTop: Boolean = true

    override def atMost: EscapeProperty = NoEscape

    final val flags = EscapeProperty.MAYBE
}

/**
 * Used when the respective object instance definitively escapes, but the final –
 * not yet available – escape level may just be [[EscapeInCallee]].
 *
 * @see [[EscapeProperty]] for further details.
 * @author Florian Kuebler
 */
case object MaybeEscapeInCallee extends EscapeProperty {
    final val isRefineable = true

    final val PID = 10

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "MaybeInCallee"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID       ⇒ true
            case EscapeInCallee.PID ⇒ true
            case MaybeNoEscape.PID  ⇒ true
            case PID                ⇒ true
            case _                  ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = EscapeInCallee

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE
}

case object MaybeEscapeViaParameter extends EscapeProperty {
    final val isRefineable = true

    final val PID = 11

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "MaybeViaParameter"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID            ⇒ true
            case EscapeInCallee.PID      ⇒ true
            case EscapeViaParameter.PID  ⇒ true
            case MaybeNoEscape.PID       ⇒ true
            case MaybeEscapeInCallee.PID ⇒ true
            case PID                     ⇒ true
            case _                       ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = EscapeViaParameter

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER
}

case object MaybeEscapeViaReturn extends EscapeProperty {
    final val isRefineable = true

    final val PID = 12

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "MaybeViaReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID            ⇒ true
            case EscapeInCallee.PID      ⇒ true
            case EscapeViaReturn.PID     ⇒ true
            case MaybeNoEscape.PID       ⇒ true
            case MaybeEscapeInCallee.PID ⇒ true
            case PID                     ⇒ true
            case _                       ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = EscapeViaReturn

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE | EscapeProperty.VIA_RETURN
}

case object MaybeEscapeViaAbnormalReturn extends EscapeProperty {
    final val isRefineable = true

    final val PID = 13

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "MaybeViaAbnormalReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID                ⇒ true
            case EscapeInCallee.PID          ⇒ true
            case EscapeViaAbnormalReturn.PID ⇒ true
            case MaybeNoEscape.PID           ⇒ true
            case MaybeEscapeInCallee.PID     ⇒ true
            case PID                         ⇒ true
            case _                           ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = EscapeViaAbnormalReturn

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE | EscapeProperty.VIA_ABNORMAL_RETURN
}

case object MaybeEscapeViaParameterAndReturn extends EscapeProperty {
    final val isRefineable = true

    final val PID = 14

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "MaybeViaParameterAndReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID                    ⇒ true
            case EscapeInCallee.PID              ⇒ true
            case EscapeViaParameter.PID          ⇒ true
            case EscapeViaReturn.PID             ⇒ true
            case EscapeViaParameterAndReturn.PID ⇒ true
            case MaybeNoEscape.PID               ⇒ true
            case MaybeEscapeInCallee.PID         ⇒ true
            case MaybeEscapeViaParameter.PID     ⇒ true
            case MaybeEscapeViaReturn.PID        ⇒ true
            case PID                             ⇒ true
            case _                               ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = EscapeViaParameterAndReturn

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER | EscapeProperty.VIA_RETURN
}

case object MaybeEscapeViaParameterAndAbnormalReturn extends EscapeProperty {
    final val isRefineable = true

    final val PID = 15

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "MaybeViaParameterAndAbnormalReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID                            ⇒ true
            case EscapeInCallee.PID                      ⇒ true
            case EscapeViaParameter.PID                  ⇒ true
            case EscapeViaAbnormalReturn.PID             ⇒ true
            case EscapeViaParameterAndAbnormalReturn.PID ⇒ true
            case MaybeNoEscape.PID                       ⇒ true
            case MaybeEscapeInCallee.PID                 ⇒ true
            case MaybeEscapeViaParameter.PID             ⇒ true
            case MaybeEscapeViaAbnormalReturn.PID        ⇒ true
            case PID                                     ⇒ true
            case _                                       ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = EscapeViaParameterAndAbnormalReturn

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER | EscapeProperty.VIA_ABNORMAL_RETURN
}

case object MaybeEscapeViaNormalAndAbnormalReturn extends EscapeProperty {
    final val isRefineable = true

    final val PID = 16

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaNormalAndAbnormalReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean =
        (that.propertyValueID: @switch) match {
            case NoEscape.PID                         ⇒ true
            case MaybeNoEscape.PID                    ⇒ true
            case EscapeInCallee.PID                   ⇒ true
            case EscapeViaAbnormalReturn.PID          ⇒ true
            case EscapeViaReturn.PID                  ⇒ true
            case EscapeViaNormalAndAbnormalReturn.PID ⇒ true
            case MaybeEscapeInCallee.PID              ⇒ true
            case MaybeEscapeViaAbnormalReturn.PID     ⇒ true
            case MaybeEscapeViaReturn.PID             ⇒ true
            case PID                                  ⇒ true
            case _                                    ⇒ false
        }

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = EscapeViaNormalAndAbnormalReturn

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE | EscapeProperty.VIA_RETURN | EscapeProperty.VIA_ABNORMAL_RETURN
}

case object MaybeEscapeViaParameterAndNormalAndAbnormalReturn extends EscapeProperty {
    final val isRefineable = true

    final val PID = 17

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "MaybeViaParameterAndNormalAndAbnormalReturn"

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean = !that.isBottom

    override def isBottom: Boolean = false

    override def isTop: Boolean = false

    override def atMost: EscapeProperty = EscapeViaParameterAndNormalAndAbnormalReturn

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER | EscapeProperty.VIA_RETURN | EscapeProperty.VIA_ABNORMAL_RETURN
}

/**
 * ''The object escapes globally, typically because it is assigned to a static variable or to a
 * field of a heap object.''
 *
 * This property should be used if and only if the analysis is conclusive and could determine
 * that the value definitively escapes globally.
 * If a more advanced analysis – potentially run later – could identify an object
 * as only [[EscapeViaParameter]], [[EscapeInCallee]] or even [[NoEscape]] then the refineable
 * property [[MaybeNoEscape]] (or another non final property) should be used.
 *
 * @example
 * Given the following library code:
 * {{{
 * public class X{
 *  public static Object o;
 *  public void m(boolean b) {
 *      Object o = new Object();        // ALLOCATION SITE
 *      if (b) X.o = o;
 *      return;
 *  }
 * }
 * }}}
 * An analysis is only expected to return [[EscapeViaStaticField]] for the object o
 * instantiated in m, if the analyses ''knows'' that m is called and the parameter b is
 * potentially `true`. If the above code is found in a library it may very well be the case that
 * certain parameter values/combinations will never be used in a certain setting and – therefore –
 * o does not escape.
 *
 * However, from a pure technical point-of-view it may be useful/necessary to use
 * [[GlobalEscape]] at some point to let depending computations know that no more
 * changes will happen and therefore the dependencies can be deleted.
 * @see [[EscapeProperty]] for further details.
 * @author Florian Kuebler
 */
trait GlobalEscape extends EscapeProperty {
    final val isRefineable = false
    override def isBottom: Boolean = true

    override def isTop: Boolean = false

    final val flags = EscapeProperty.MAYBE | EscapeProperty.IN_CALLEE | EscapeProperty.VIA_PARAMETER | EscapeProperty.VIA_RETURN | EscapeProperty.VIA_ABNORMAL_RETURN | EscapeProperty.GLOBAL

    override def lessOrEqualRestrictive(that: EscapeProperty): Boolean = true

    override def atMost: EscapeProperty = this
}

case object GlobalEscape extends GlobalEscape {

    final val PID = 18

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "Global"

    override def meet(that: EscapeProperty): EscapeProperty = this
}

/**
 * The object is assigned to a (global) heap object. (It may additionally escape by other
 * means too, but this property was derived first.)
 *
 * @example
 * Given the following code:
 * {{{
 * public class X{
 *  public static X o = new X();
 *  public Object f;
 *  public void m() {
 *      Object o = new Object();        // ALLOCATION SITE
 *      X x = X.o;
 *      x.f = o;
 *      return;
 *  }
 * }
 * }}}
 *
 * @see [[GlobalEscape]] for further details.
 * @author Florian Kuebler
 */
case object EscapeViaHeapObject extends GlobalEscape {

    final val PID = 19

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaHeapObject"

    override def meet(that: EscapeProperty): EscapeProperty =
        if (that.propertyValueID == EscapeViaStaticField.PID || that.propertyValueID == GlobalEscape.PID)
            GlobalEscape
        else this
}

/**
 * Characterizes escapes via the write to a static field. (It may additionally escape by other
 * means too, but this property was derived first.)
 *
 * @example
 * Given the following code:
 * {{{
 * public class X{
 *  public static Object o;
 *  public void m() {
 *      Object o = new Object();        // ALLOCATION SITE
 *      X.o = o;
 *      return;
 *  }
 * }
 * }}}
 *
 * @see [[GlobalEscape]] for further details.
 * @author Florian Kuebler
 */
case object EscapeViaStaticField extends GlobalEscape {

    final val PID = 20

    override def propertyValueID: PropertyKeyID = PID

    override def propertyName: String = "ViaStaticField"

    override def meet(that: EscapeProperty): EscapeProperty =
        if (that.propertyValueID == EscapeViaHeapObject.PID || that.propertyValueID == GlobalEscape.PID)
            GlobalEscape
        else this
}
