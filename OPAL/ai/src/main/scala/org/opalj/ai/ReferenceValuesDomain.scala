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

import org.opalj.br._

/**
 *
 * @author Michael Eichberg (eichberg@informatik.tu-darmstadt.de)
 */
trait ReferenceValuesDomain { this: CoreDomain ⇒

    // -----------------------------------------------------------------------------------
    //
    // FACTORY METHODS TO CREATE REFERENCE VALUES
    //
    // -----------------------------------------------------------------------------------

    final def justThrows(value: ExceptionValue): ThrowsException[ExceptionValues] =
        ThrowsException(Seq(value))

    final def throws(value: ExceptionValue): ThrowsException[ExceptionValue] =
        ThrowsException(value)

    /**
     * Factory method to create a `DomainValue` that represents the `null` value and
     * and that was created (explicitly or implicitly) by the instruction (`aconst_null`)
     * with the specified program counter.
     *
     * The domain may ignore the information about the value and the origin (`pc`).
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: N/A
     *  - Type: '''Null'''
     *  - Null: '''Yes'''
     */
    def NullValue(vo: ValueOrigin): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents ''either a reference
     * value that has the given type and is initialized or the value `null`''. However, the
     * information whether the value is `null` or not is not available. Furthermore, the
     * type may also just be an upper bound.
     *
     * The domain may ignore the information about the value and the origin, but
     * it has to remain possible for the domain to identify the component type of an
     * array.
     *
     * ==Summary==
     * The properties of the domain value are:
     *
     *  - Initialized: '''Yes''' (if non-null the constructor was called/the array was initialized)
     *  - Type: '''Upper Bound'''
     *  - Null: '''Unknown'''
     *  - Content: '''Unknown'''
     */
    def ReferenceValue(origin: ValueOrigin, referenceType: ReferenceType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents ''an array''
     * that was successfully created and which has the given type.
     *
     * The domain may ignore the information about the origin (`pc`) and
     * the precise size of each dimension.
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes'''
     *  	(i.e., the fields of the array have the type dependent default value)
     *  - Type: '''Precise'''
     *  - Null: '''No'''
     *  - Content: '''Unknown'''
     *
     * @param origin Information about the origin of the value.
     * @param counts The size of each dimension if available. `counts` may be empty (`Nil`)
     * 		if no corresponding information is available; however, if available the
     *   	following condition always has to hold: `counts.length <= arrayType.dimensions`.
     */
    def InitializedArrayValue(
        origin: ValueOrigin,
        counts: List[Int],
        arrayType: ArrayType): DomainValue

    /**
     * Represents ''a non-null reference value with the given type as an upper type bound''.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes''' (the constructor was called)
     *  - Type: '''Upper Bound'''
     *  - Null: '''No''' (This value is not `null`.)
     */
    def NonNullObjectValue(vo: ValueOrigin, objectType: ObjectType): DomainValue

    /**
     * Creates a new `DomainValue` that represents ''a new,
     * uninitialized instance of an object of the given type''. The object was
     * created by the (`NEW`) instruction with the specified program counter.
     *
     * OPAL calls this method when it evaluates `newobject` instructions.
     * If the bytecode is valid a call of one of the object's constructors will
     * subsequently initialize the object.
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''no''' (only the memory is allocated for the object)
     *  - Type: '''precise''' (i.e., this type is not an upper bound,
     *      the type correctly models the runtime type.)
     *  - Null: '''No''' (This value is not `null`.)
     *
     * @note Instances of arrays are created by the `newarray` and
     *      `multianewarray` instructions and in both cases an exception may be thrown
     *      (e.g., `NegativeArraySizeException`).
     */
    def NewObject(vo: ValueOrigin, objectType: ObjectType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents an '''initialized'''
     * reference value of the given type and that was created (explicitly or implicitly)
     * by the instruction with the specified program counter.
     *
     * ==General Remarks==
     * The given type usually identifies a class type (not an interface type) that is
     * not abstract, but in some cases (e.g. consider `java.awt.Toolkit()`)
     * it may be useful/meaningful to relax this requirement and to state that the
     * class precisely represents the runtime type – even
     * so the class is abstract. However, such decisions need to be made by the domain.
     *
     * This method is used by the OPAL framework to create reference values that are normally
     * internally created by the JVM (in particular exceptions such as
     * `NullPointExeception` and `ClassCastException`). However, it can generally
     * be used to create initialized objects/arrays.
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes'''
     *  - Type: '''precise''' (i.e., this type is not an upper bound, the type
     *      correctly models the runtime type.)
     *  - Null: '''No''' (This value is not `null`.)
     */
    def InitializedObjectValue(vo: ValueOrigin, objectType: ObjectType): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents the given string value
     * and that was created by the instruction with the specified program counter.
     *
     * This function is called by OPAL-AI when a string constant (`LDC(_W)` instruction) is
     * put on the stack.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes''' and the String's value is the given value. The string
     *      may be empty (""), but it is never `null`.
     *  - Type: '''java.lang.String'''
     *  - Null: '''No'''
     *
     * @param value A non-null string. (The string may be empty, though.)
     */
    def StringValue(vo: ValueOrigin, value: String): DomainValue

    /**
     * Factory method to create a `DomainValue` that represents a runtime value of
     * type "`Class&lt;T&gt;`" and that was created by the instruction with the
     * specified program counter.
     *
     * This function is called by OPAL when a class constant (`LDC(_W)` instruction) is
     * put on the stack.
     *
     * The domain may ignore the information about the value and the origin (`vo`).
     *
     * ==Summary==
     * The properties of the domain value are:
     *  - Initialized: '''Yes''' and the type represented by the class is the given type.
     *  - Type: '''java.lang.Class<t:Type>'''
     *  - Null: '''No'''
     */
    def ClassValue(vo: ValueOrigin, t: Type): DomainValue

    /**
     * Called by the AI framework for each load constant method handle instruction to
     * get a representation of/a DomainValue that represents the handle.
     *
     * @param handle A valid method handle.
     * @return An `InitializedObjectValue(ObjectType.MethodHandle)`.
     *      Hence, this method needs to be overridden
     *      if resolution of MethodHandle based method calls should be performed.
     */
    def MethodHandle(pc: PC, handle: MethodHandle): DomainValue =
        InitializedObjectValue(pc, ObjectType.MethodHandle)

    /**
     * Called by the AI framework for each load constant method type instruction to
     * get a domain-specific representation of the method descriptor as a `MethodType`.
     *
     * @param handle A valid method descriptor.
     * @return An `InitializedObjectValue(ObjectType.MethodType)`.
     *      Hence, this method needs to be overridden
     *      if resolution of MethodType based method calls should be performed.
     */
    def MethodType(pc: PC, descriptor: MethodDescriptor): DomainValue =
        InitializedObjectValue(pc, ObjectType.MethodType)

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Tries to determine if the type referred to as `subtype` is a subtype of the
     * specified reference type `supertype`.
     */
    /*ABSTRACT*/ def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer

    /**
     * Tries to determine – under the assumption that the given `value` is not `null` – 
     * if the runtime type of the given reference value could be a
     * subtype of the specified reference type `supertype`. I.e., if the type of the
     * value is not precisely known, then all subtypes of the `value`'s type are also
     * taken into consideration when analyzing the subtype relation and only if we
     * can guarantee that none is a subtype of the given `supertype` the answer will be
     * `No`.
     *
     * @note The returned value is only meaningful if `value` does not represent
     *      the runtime value `null`.
     */
    /*ABSTRACT*/ def isValueSubtypeOf(
        value: DomainValue,
        supertype: ReferenceType): Answer

    /**
     * Determines whether the given value is `null` (`Yes`), maybe `null` (`Unknown`) or
     * is not `null` (`No`).
     *
     * @param value A value of computational type reference.
     */
    /*ABSTRACT*/ def refIsNull(value: DomainValue): Answer

    def refIsNonNull(value: DomainValue): Answer = refIsNull(value).negate

    /**
     * Compares the given values for reference equality. Returns `Yes` if both values
     * point to the '''same instance''' and returns `No` if both objects are known not to
     * point to the same instance. The latter is, e.g., trivially the case when both
     * values have a different concrete type. Otherwise `Unknown` is returned.
     *
     * @param value1 A value of computational type reference.
     * @param value2 A value of computational type reference.
     */
    /*ABSTRACT*/ def refAreEqual(value1: DomainValue, value2: DomainValue): Answer

    def refAreNotEqual(value1: DomainValue, value2: DomainValue): Answer =
        refAreEqual(value1, value2).negate

    // -----------------------------------------------------------------------------------
    //
    // HANDLING CONSTRAINTS RELATED TO VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // W.r.t Reference Values
    /**
     * Called by the framework when the value is known to be `null`/has to be `null`.
     * E.g., after a comparison with `null` (IFNULL/IFNONNULL) OPAL-AI knows that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def refEstablishIsNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def RefIsNull = refEstablishIsNull _

    /**
     * Called by OPAL-AI when it establishes that the value is guaranteed not to be `null`.
     * E.g., after a comparison with `null` OPAL can establish that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def refEstablishIsNonNull(
        pc: PC,
        value: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def RefIsNonNull = refEstablishIsNonNull _

    /**
     * Called by OPAL when two values were compared for reference equality and
     * we are going to analyze the branch where the comparison succeeded.
     */
    def refEstablishAreEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def RefAreEqual = refEstablishAreEqual _

    def refEstablishAreNotEqual(
        pc: PC,
        value1: DomainValue,
        value2: DomainValue,
        operands: Operands,
        locals: Locals): (Operands, Locals) = (operands, locals)
    private[ai] final def RefAreNotEqual = refEstablishAreNotEqual _

    /**
     * Called by the abstract interpreter when '''the type bound of the top most stack
     * value needs to be refined'''. This method is only called by the abstract
     * interpreter iff an immediately preceding subtype query (typeOf(value) <: bound)
     * returned `Unknown` and must not be ignored – w.r.t. the top-most stack value –
     * by the value.
     *
     * A domain that is able to identify aliases can use this information to propagate
     * the information to the other aliases.
     */
    /*abstract*/ def refEstablishUpperBound(
        pc: PC,
        bound: ReferenceType,
        operands: Operands,
        locals: Locals): (Operands, Locals)

    // -----------------------------------------------------------------------------------
    //
    // ABSTRACTIONS RELATED TO INSTRUCTIONS
    //
    // -----------------------------------------------------------------------------------

    //
    // METHODS TO IMPLEMENT THE SEMANTICS OF INSTRUCTIONS
    //

    //
    // CREATE ARRAY
    //

    /**
     * The return value is either a new array or a `NegativeArraySizeException` if
     * count is negative.
     */
    def newarray(
        pc: PC,
        count: DomainValue,
        componentType: FieldType): Computation[DomainValue, ExceptionValue]

    /**
     * The return value is either a new array or a `NegativeArraySizeException` if
     * count is negative.
     */
    def multianewarray(
        pc: PC,
        counts: List[DomainValue],
        arrayType: ArrayType): Computation[DomainValue, ExceptionValue]

    //
    // LOAD FROM AND STORE VALUE IN ARRAYS
    //

    /**
     * Computation that returns the value stored in an array at a given index or an
     * exception. The exceptions that may be thrown are: `NullPointerException` and
     * `ArrayIndexOutOfBoundsException`.
     */
    type ArrayLoadResult = Computation[DomainValue, ExceptionValues]
    /**
     * Computation that succeeds (updates the value stored in the array at the given
     * index) or that throws an exception. The exceptions that may be thrown are:
     * `NullPointerException`, `ArrayIndexOutOfBoundsException` and `ArrayStoreException`.
     */
    type ArrayStoreResult = Computation[Nothing, ExceptionValues]

    //
    // STORING VALUES IN AND LOADING VALUES FROM ARRAYS
    //

    def aaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def aastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def baload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def bastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def caload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def castore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def daload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def dastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def faload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def fastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def iaload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def iastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def laload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def lastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    def saload(pc: PC, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def sastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult

    //
    // LENGTH OF AN ARRAY
    //

    /**
     * Returns the array's length or throws a `NullPointerException`.
     */
    def arraylength(
        pc: PC,
        arrayref: DomainValue): Computation[DomainValue, ExceptionValue]

    /**
     * Handles a `monitorenter` instruction.
     */
    def monitorenter(pc: PC, value: DomainValue): Computation[Nothing, ExceptionValue]

    /**
     * Handles a `monitorexit` instruction.
     */
    def monitorexit(pc: PC, value: DomainValue): Computation[Nothing, ExceptionValue]

}
