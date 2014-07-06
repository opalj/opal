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
package ai

import scala.reflect.ClassTag

import org.opalj.util.{ Answer, Yes, No, Unknown }

import br._

/**
 * A domain is the fundamental abstraction mechanism in OPAL that enables the customization
 * of the abstract interpretation framework towards the needs of a specific analysis.
 *
 * A domain encodes the semantics of computations (e.g., the addition of two values)
 * with respect to a domain's values (e.g., the representation of integer values).
 * Customizing a domain is the fundamental mechanism of adapting the AI framework
 * to one's needs.
 *
 * This trait defines the interface between the abstract interpretation framework
 * and some (user defined) domain. I.e., this interface defines all methods that
 * are needed by OPAL to perform an abstract interpretation.
 *
 * ==Control Flow==
 * OPAL controls the process of evaluating the code of a method, but requires a
 * domain to perform the actual computations of an instruction's result. E.g., to
 * calculate the result of adding two integer values, or to perform the comparison
 * of two object instances, or to get the result of converting a `long` value to an
 * `int` value the framework always consults the domain.
 *
 * Handling of instructions that manipulate the stack (e.g. `dup`), that move values
 * between the stack and the locals (e.g., `Xload_Y`) or that determine the control
 * flow is, however, completely embedded into OPAL-AI.
 *
 * OPAL uses the following three methods to inform a domain about the progress of the
 * abstract interpretation:
 *  - [[org.opalj.ai.CoreDomain.flow]]
 *  - [[org.opalj.ai.CoreDomain.evaluationCompleted]]
 *  - [[org.opalj.ai.CoreDomain.abstractInterpretationEnded]]
 * A domain that implements (`overrides`) one of these methods should always also delegate
 * the call to its superclass to make sure that every domain interested in these
 * events is informed.
 *
 * ==Implementing Abstract Domains==
 * While it is perfectly possible to implement a new domain by inheriting from this
 * trait, it is recommended  to first study the already implemented domains and to
 * use them as a foundation.
 * To facilitate the usage of OPAL several classes/traits that implement parts of
 * this `Domain` trait are pre-defined and can be flexibly combined (mixed together)
 * when needed.
 *
 * When you extend this trait or implement parts of it you should keep as many methods/
 * fields private to facilitate mix-in composition of multiple traits.
 *
 * ==Thread Safety==
 * When every analyzed method is associated with a unique `Domain` instance and – given
 * that OPAL only uses one thread to analyze a given method at a time – no special care
 * has to be taken. However, if a domain needs to consult another domain which is, e.g,
 * associated with a project as a whole, it is then the responsibility of the domain to
 * make sure that coordination with the world is thread safe.
 *
 * @note OPAL assumes that – at least conceptually – every method/code block is associated
 *      with its own instance of a domain object.
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 * @author Dennis Siebert
 */
trait Domain
        extends CoreDomain
        with IntegerValuesDomain
        with LongValuesDomain
        with FloatValuesDomain
        with DoubleValuesDomain
        with ReferenceValuesDomain
        with FieldAccessesDomain
        with MethodCallsDomain {

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS TO CREATE DOMAIN VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Factory method to create domain values with a specific type. I.e., values for
     * which we have some type information but no value or source information.
     * However, the value is guaranteed to be proper initialized (if non-null).
     *
     * For example, if `valueType` is a reference type it may be possible
     * that the actual value is `null`, but such knowledge is not available.
     *
     * The framework uses this method when a method is to be analyzed, but no parameter
     * values are given and initial values need to be generated. This method is not
     * used elsewhere by the framework.
     */
    def TypedValue(vo: ValueOrigin, valueType: Type): DomainValue = valueType match {
        case BooleanType       ⇒ BooleanValue(vo)
        case ByteType          ⇒ ByteValue(vo)
        case ShortType         ⇒ ShortValue(vo)
        case CharType          ⇒ CharValue(vo)
        case IntegerType       ⇒ IntegerValue(vo)
        case FloatType         ⇒ FloatValue(vo)
        case LongType          ⇒ LongValue(vo)
        case DoubleType        ⇒ DoubleValue(vo)
        case rt: ReferenceType ⇒ ReferenceValue(vo, rt)
        case VoidType ⇒
            throw DomainException("a domain value cannot have the type void")
    }

    /**
     * Creates the domain value that represents the constant field value.
     */
    final def ConstantFieldValue(pc: PC, cv: ConstantFieldValue[_]): DomainValue = {
        (cv.kindId: @scala.annotation.switch) match {
            case ConstantInteger.KindId ⇒ IntegerValue(pc, cv.toInt)
            case ConstantLong.KindId    ⇒ LongValue(pc, cv.toLong)
            case ConstantFloat.KindId   ⇒ FloatValue(pc, cv.toFloat)
            case ConstantDouble.KindId  ⇒ DoubleValue(pc, cv.toDouble)
            case ConstantString.KindId  ⇒ StringValue(pc, cv.toUTF8)
        }
    }

    /**
     * Creates a `DomainValue` that represents a a value with the given type
     * and whic is initialized using the JVM's default value for that type.
     * E.g., for `IntegerValue`s the value is set to `0`. In case of a
     * `ReferenceType` the value is the [[NullValue]].
     */
    final def DefaultValue(pc: PC, theType: FieldType): DomainValue =
        theType match {
            case BooleanType      ⇒ BooleanValue(pc, false)
            case ByteType         ⇒ ByteValue(pc, 0)
            case CharType         ⇒ CharValue(pc, 0)
            case ShortType        ⇒ ShortValue(pc, 0)
            case IntegerType      ⇒ IntegerValue(pc, 0)
            case FloatType        ⇒ FloatValue(pc, 0.0f)
            case LongType         ⇒ LongValue(pc, 0l)
            case DoubleType       ⇒ DoubleValue(pc, 0.0d)
            case _: ReferenceType ⇒ NullValue(pc)
        }

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // INTER-TYPE CONVERSIONS
    //
    def i2d(pc: PC, value: DomainValue): DomainValue
    def i2f(pc: PC, value: DomainValue): DomainValue
    def i2l(pc: PC, value: DomainValue): DomainValue

    /** Conversion of the given long value to a double value. */
    def l2d(pc: PC, value: DomainValue): DomainValue
    /** Conversion of the given long value to a float value. */
    def l2f(pc: PC, value: DomainValue): DomainValue
    /** Conversion of the given long value to an integer value. */
    def l2i(pc: PC, value: DomainValue): DomainValue

    def f2d(pc: PC, value: DomainValue): DomainValue
    def f2i(pc: PC, value: DomainValue): DomainValue
    def f2l(pc: PC, value: DomainValue): DomainValue

    def d2f(pc: PC, value: DomainValue): DomainValue
    def d2i(pc: PC, value: DomainValue): DomainValue
    def d2l(pc: PC, value: DomainValue): DomainValue

    //
    // RETURN FROM METHOD
    //
    /**
     * Called by OPAL-AI when a return instruction with the given `pc` is reached.
     * In other words, when the method returns normally.
     */
    def returnVoid(pc: PC): Unit

    /**
     * The given `value`, which is a value with ''computational type integer'', is returned
     * by the return instruction with the given `pc`.
     */
    def ireturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type long'', is returned
     * by the return instruction with the given `pc`.
     */
    def lreturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type float'', is returned
     * by the return instruction with the given `pc`.
     */
    def freturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type double'', is returned
     * by the return instruction with the given `pc`.
     */
    def dreturn(pc: PC, value: DomainValue): Unit

    /**
     * The given `value`, which is a value with ''computational type reference'', is returned
     * by the return instruction with the given `pc`.
     */
    def areturn(pc: PC, value: DomainValue): Unit

    /**
     * Called by the abstract interpreter when an exception is thrown that is not
     * (guaranteed to be) handled within the same method.
     *
     * @note If the original exception value is `null` (`/*E.g.*/throw null;`), then
     *      the exception that is actually thrown is a new `NullPointerException`. This
     *      situation is, however, completely handled by OPAL and the exception value
     *      is hence never `null`.
     */
    def abruptMethodExecution(pc: PC, exceptionValue: ExceptionValue): Unit

    //
    // "OTHER" INSTRUCTIONS
    //

    /**
     * Handles a `monitorenter` instruction.
     */
    def monitorenter(pc: PC, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * Handles a `monitorexit` instruction.
     */
    def monitorexit(pc: PC, value: DomainValue): Computation[Nothing, ExceptionValue]

}
