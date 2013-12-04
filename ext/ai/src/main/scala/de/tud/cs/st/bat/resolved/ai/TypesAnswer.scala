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

import de.tud.cs.st.util.Answer

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
case object HasUnknownType extends TypesAnswer

/**
 * The value has the primitive type.
 */
sealed trait IsPrimitiveValue extends TypesAnswer {
    def primitiveType: BaseType
}

object IsPrimitiveValue {
    def unapplay(answer: IsPrimitiveValue): Option[BaseType] = Some(answer.primitiveType)
}

case object IsBooleanValue extends IsPrimitiveValue { final val primitiveType = BooleanType }

case object IsByteValue extends IsPrimitiveValue { final val primitiveType = ByteType }

case object IsCharValue extends IsPrimitiveValue { final val primitiveType = CharType }

case object IsShortValue extends IsPrimitiveValue { final val primitiveType = ShortType }

case object IsIntegerValue extends IsPrimitiveValue { final val primitiveType = IntegerType }

case object IsFloatValue extends IsPrimitiveValue { final val primitiveType = FloatType }

case object IsLongValue extends IsPrimitiveValue { final val primitiveType = LongType }

case object IsDoubleValue extends IsPrimitiveValue { final val primitiveType = DoubleType }

/**
 * The value is a reference value.
 *
 * @author Michael Eichberg
 */
trait IsReferenceValue extends TypesAnswer {

    /**
     * In general a domain value can represent several distinct values (depending
     * on the control flow). Each of these values can have a different upper bound and
     * an upper bound can consist of several interfaces and a class.
     */
    def upperBounds: Iterable[ValueBasedUpperBound]

    def hasSingleBound: Option[ReferenceType]
}

/**
 * Defines an extractor method for instances of `IsReferenceValue` objects.
 *
 * @author Michael Eichberg
 */
object IsReferenceValue {

    def unapply(answer: IsReferenceValue): Option[Iterable[ValueBasedUpperBound]] = {
        Some(answer.upperBounds)
    }
}

/**
 * Defines an extractor method for instances of `IsReferenceValue` objects.
 *
 * @author Michael Eichberg
 */
object IsReferenceValueWithSingleBound {

    def unapply(answer: IsReferenceValue): Option[ReferenceType] =
        answer.hasSingleBound

}

/**
 * The upper bound of a single value. Captures the information about one of the values
 * a domain value may refer to.
 *
 * @author Michael Eichberg
 */
trait ValueBasedUpperBound {

    def isNull: Answer

    def isPrecise: Boolean

    def upperBound: UpperBound

    /**
     * @note The function `isSubtypeOf` is not determined if `isNull` returns `Yes`;
     *      if `isNull` is `Unknown` then the result is given under the
     *      assumption that the value is not `null` at runtime.
     */
    def isSubtypeOf(referenceType: ReferenceType): Answer
}

