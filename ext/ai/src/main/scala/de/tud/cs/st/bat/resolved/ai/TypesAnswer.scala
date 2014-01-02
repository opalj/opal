/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai

import de.tud.cs.st.util.{ Answer, Yes }

/**
 * The answer of a domain to a query about a value's specific type.
 *
 * (See `Domain.valueOfType(DomainValue)` for further details.)
 *
 * @author Michael Eichberg
 */
sealed trait TypesAnswer

/**
 * This answer is given when no specific/additional type information about a value
 * is available.
 *
 * @note Recall that the computational type of a value always has to be available, but
 *      that a `Domain.typeOfValue(...)` query does not need to take the computational type
 *      into account (Whenever BATAI requires the computational type of a value it uses
 *      the respective method.)
 *
 * @author Michael Eichberg
 */
case object TypeUnknown extends TypesAnswer

/**
 * The value has the primitive type.
 */
sealed trait IsPrimitiveValue extends TypesAnswer {
    def primitiveType: BaseType
}

object IsPrimitiveValue {
    def unapply(answer: IsPrimitiveValue): Option[BaseType] =
        Some(answer.primitiveType)
}

trait IsBooleanValue extends IsPrimitiveValue {
    final def primitiveType = BooleanType
}

trait IsByteValue extends IsPrimitiveValue {
    final def primitiveType = ByteType
}

trait IsCharValue extends IsPrimitiveValue {
    final def primitiveType = CharType
}

trait IsShortValue extends IsPrimitiveValue {
    final def primitiveType = ShortType
}

trait IsIntegerValue extends IsPrimitiveValue {
    final def primitiveType = IntegerType
}

trait IsFloatValue extends IsPrimitiveValue {
    final def primitiveType = FloatType
}

trait IsLongValue extends IsPrimitiveValue {
    final def primitiveType = LongType
}

trait IsDoubleValue extends IsPrimitiveValue {
    final def primitiveType = DoubleType
}

/**
 * The value is a reference value.
 *
 * @author Michael Eichberg
 */
trait IsReferenceValue extends TypesAnswer with IsAReferenceValue {

    /**
     * In general a domain value can represent several distinct values (depending
     * on the control flow). Each of these values can have a different upper bound and
     * an upper bound can consist of several interfaces and a class.
     */
    def referenceValues: Iterable[IsAReferenceValue] // TODO make iterator!!!

}

/**
 * Defines an extractor method for instances of `IsReferenceValue` objects.
 *
 * @author Michael Eichberg
 */
object IsReferenceValue {

    def unapply(value: IsReferenceValue): Option[Iterable[IsAReferenceValue]] = {
        Some(value.referenceValues)
    }
}

/**
 * Characterizes a single reference value. Captures the information about one of the values
 * a domain value may refer to. For example, in the following:
 * {{{
 * val o = If(...) new Object() else "STRING"
 * }}}
 * o is a reference value (`IsReferenceValue`) that refers to two "primitive" values each
 * represented by an instance of an `IsAReferenceValue` (`new Object()` and `"STRING"`).
 *
 * @author Michael Eichberg
 */
trait IsAReferenceValue {

    /**
     * If `Yes` the value is statically known to be `null` at runtime. In this
     * case the upper bound is (has to be) empty. If the answer is `Unknown` then the
     * analysis was not able to statically determine whether the value is `null` or
     * is not `null`. In this case the upper bound is expected to be non-empty.
     * If the answer is `No` then the value is statically known not to be `null`. In this
     * case, the upper bound may precisely identify the runtime type or still just
     * identify an upper bound.
     */
    def isNull: Answer

    /**
     * Returns `true` if the type information is precise. I.e., the type precisely
     * models the runtime type of the value.
     *
     * @note If value is known to be `null`, `isPrecise` will also return `true`.
     */
    def isPrecise: Boolean

    /**
     * The upper bound of the value's type. The upper bound is empty if this
     * value is `null` (i.e., `isNUll == Yes`). The upper bound will only contain
     * a single type if the type is precise. (i.e., `isPrecise == true`). Otherwise,
     * the upper type bound will contain one or more types that are not known to be
     * in an inheritance relation, but which will ''correctly'' approximate the runtime
     * type.
     *
     * @note If only a part of a project is analyzed, the class hierarchy may be
     *      fragmented and it may happen that two classes that are indeed in an
     *      inheritance relation if we would analyzed the complete project are reported
     *      in the upper type bound.
     */
    def upperTypeBound: UpperTypeBound

    /**
     * Checks if the type of this value is a subtype of the specified
     * reference type under the assumption that this value is not `null`!
     *
     * Basically, this method implements the same semantics as the `ClassHierarchy`'s
     * `isSubtypeOf` method, but it additionally  checks if the type of this value
     * '''could be a subtype'' of the given supertype. I.e., if this value's type
     * identifies a supertype of the given `supertype` the answer is unknown.
     *
     * For example, assume that the type of this reference value is
     * `java.util.Collection` and we know/have to assume that this is only an
     * upper bound. In this case an answer is `No` if and only if it is impossible
     * that the runtime type is a subtype of the given supertype. This
     * condition holds, for example, for `java.io.File` which is not a subclass
     * of `java.util.Collection` and which does not have any further subclasses (in
     * the JDK). I.e., the classes `java.io.File` and `java.util.Collection` are
     * not in an inheritance relationship. However, if the specified supertype would
     * be `java.util.List` the answer would be unknown.
     *
     * @note The function `isSubtypeOf` is not defined if `isNull` returns `Yes`;
     *      if `isNull` is `Unknown` then the result is given under the
     *      assumption that the value is not `null` at runtime.
     */
    def isValueSubtypeOf(referenceType: ReferenceType): Answer
}

/**
 * @author Michael Eichberg
 */
object IsNullValue {

    def unapply(rv: IsReferenceValue): Boolean = rv.isNull == Yes

}
