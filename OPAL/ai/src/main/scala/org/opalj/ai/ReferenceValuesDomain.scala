/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.ReferenceType
import org.opalj.br.ArrayType
import org.opalj.br.FieldType

/**
 * Domain that defines all methods that perform computations related to `RefernceValues`.
 *
 * @author Michael Eichberg
 */
trait ReferenceValuesDomain extends ReferenceValuesFactory { domain =>

    // -----------------------------------------------------------------------------------
    //
    // QUESTION'S ABOUT VALUES
    //
    // -----------------------------------------------------------------------------------

    /**
     * Tries to determine if the type referred to as `subtype` is a subtype of the
     * specified reference type `supertype`. If the class hierarchy is not complete
     * the answer may be Unknown.
     */
    /*ABSTRACT*/ def isASubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Answer

    /**
     * Returns `true` if `subtype` is a known subtype of `supertype`. If the typing relation
     * is unknown OR `subtype` is not a subtype of `supertype` `false` is returned.
     */
    /*ABSTRACT*/ def isSubtypeOf(subtype: ReferenceType, supertype: ReferenceType): Boolean

    /**
     * Tries to determine – '''under the assumption that the given `value` is not
     * `null`''' – if the runtime type of the given reference value could be a
     * subtype of the specified reference type `supertype`. I.e., if the type of the
     * value is not precisely known, then all subtypes of the `value`'s type are also
     * taken into consideration when analyzing the subtype relation and only if we
     * can guarantee that none is a subtype of the given `supertype` the answer will be
     * `No`.
     *
     * @note   The returned value is only meaningful if `value` does not represent
     *         the runtime value `null`.
     */
    /*ABSTRACT*/ def isValueASubtypeOf(
        value:     DomainValue,
        supertype: ReferenceType
    ): Answer

    /**
     * Determines whether the given value is `null` (`Yes`), maybe `null` (`Unknown`) or
     * is not `null` (`No`).
     *
     * @param value A value of computational type reference.
     */
    /*ABSTRACT*/ def refIsNull(pc: Int, value: DomainValue): Answer

    /**
     * Returns `Yes` if given value is never `null`, `Unknown` if the values is maybe
     * `null` and `No` otherwise.
     *
     * @param value A value of computational type reference.
     */
    def refIsNonNull(pc: Int, value: DomainValue): Answer = refIsNull(pc, value).negate

    /**
     * Compares the given values for reference equality. Returns `Yes` if both values
     * point to the '''same instance''' and returns `No` if both objects are known not to
     * point to the same instance. The latter is, e.g., trivially the case when both
     * values have a different concrete type. Otherwise `Unknown` is returned.
     *
     * If both values are representing the `null` value the [[org.opalj.Answer]] is `Yes`.
     *
     * @param value1 A value of computational type reference.
     * @param value2 A value of computational type reference.
     */
    /*ABSTRACT*/ def refAreEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer

    /**
     * Compares the given values for reference inequality. Returns `No` if both values
     * point to the '''same instance''' and returns `Yes` if both objects are known not to
     * point to the same instance. The latter is, e.g., trivially the case when both
     * values have a different concrete type. Otherwise `Unknown` is returned.
     *
     * If both values are representing the `null` value the [[org.opalj.Answer]] is `Yes`.
     *
     * @param value1 A value of computational type reference.
     * @param value2 A value of computational type reference.
     */
    def refAreNotEqual(pc: Int, value1: DomainValue, value2: DomainValue): Answer = {
        refAreEqual(pc, value1, value2).negate
    }

    // -----------------------------------------------------------------------------------
    //
    // HANDLING CONSTRAINTS RELATED TO MULTIPLE VALUES
    //
    // -----------------------------------------------------------------------------------

    //
    // W.r.t Reference Values

    /**
     * Sets the `is null` property of the top-most stack value to `Yes`. This method is
     * called by the framework when the top-most operand stack value '''has to be null''', but
     * a previous `isNull` check returned [[Unknown]].
     * E.g., after a [[org.opalj.br.instructions.CHECKCAST]] that fails or if a `InstanceOf`
     * check has succeeded.
     *
     * This method can be ignored; i.e., the return value can be `(operands,locals)`.
     * However, a domain that tracks alias information can use this information to propagate
     * the information to the other aliases.
     */
    def refTopOperandIsNull(
        pc:       Int,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals)

    /**
     * Called by the abstract interpreter when '''the type bound of the top most stack
     * value needs to be refined'''. This method is only called by the abstract
     * interpreter iff an immediately preceding subtype query (typeOf(value) <: bound)
     * returned `Unknown`. '''This method must not be ignored – w.r.t. refining the top-most
     * stack value'''; it is e.g., used by [[org.opalj.br.instructions.CHECKCAST]]
     * instructions.
     *
     * A domain that is able to identify aliases can use this information to propagate
     * the information to the other aliases.
     */
    /*abstract*/ def refSetUpperTypeBoundOfTopOperand(
        pc:       Int,
        bound:    ReferenceType,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals)

    /**
     * Called by the framework when the value is known to be `null`/has to be `null`.
     * E.g., after a comparison with `null` (IFNULL/IFNONNULL) OPAL-AI knows that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def refEstablishIsNull(
        pc:       Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

    /**
     * Called by OPAL-AI when it establishes that the value is guaranteed not to be `null`.
     * E.g., after a comparison with `null` OPAL can establish that the
     * value has to be `null` on one branch and that the value is not `null` on the
     * other branch.
     */
    def refEstablishIsNonNull(
        pc:       Int,
        value:    DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

    /**
     * Called by OPAL when two values were compared for reference equality and
     * we are going to analyze the branch where the comparison succeeded.
     */
    def refEstablishAreEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

    /**
     * Called by OPAL when two values were compared for reference equality and
     * we are going to analyze the branch where the comparison failed.
     */
    def refEstablishAreNotEqual(
        pc:       Int,
        value1:   DomainValue,
        value2:   DomainValue,
        operands: Operands,
        locals:   Locals
    ): (Operands, Locals) = (operands, locals)

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
        pc:            Int,
        count:         DomainValue,
        componentType: FieldType
    ): Computation[DomainValue, ExceptionValue]

    /**
     * Creates a representation of a new multidimensional array.
     * The return value is either a new array or a `NegativeArraySizeException` if
     * count is negative.
     */
    def multianewarray(
        pc:        Int,
        counts:    Operands,
        arrayType: ArrayType
    ): Computation[DomainValue, ExceptionValue]

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

    def aaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def aastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult

    def baload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def bastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult

    def caload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def castore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult

    def daload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def dastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult

    def faload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def fastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult

    def iaload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def iastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult

    def laload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def lastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult

    def saload(pc: Int, index: DomainValue, arrayref: DomainValue): ArrayLoadResult

    def sastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult

    //
    // LENGTH OF AN ARRAY
    //

    /**
     * Returns the array's length or throws a `NullPointerException`.
     */
    def arraylength(
        pc:       Int,
        arrayref: DomainValue
    ): Computation[DomainValue, ExceptionValue]

}
