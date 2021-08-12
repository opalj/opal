/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * This (partial-)domain abstracts over the concrete methods for performing
 * array operations and provides an interface at a higher abstraction level.
 *
 * @author Michael Eichberg
 */
trait GeneralizedArrayHandling extends ReferenceValuesDomain { this: ValuesDomain =>

    //
    // NEW INTERFACE
    //

    /*abstract*/ def arrayload(
        pc:       Int,
        index:    DomainValue,
        arrayRef: DomainValue
    ): ArrayLoadResult

    /*abstract*/ def arraystore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayRef: DomainValue
    ): ArrayStoreResult

    //
    // IMPLEMENTATION OF DOMAIN'S "ARRAY METHODS"
    //

    /*base impl.*/ def aaload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def aastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult =
        arraystore(pc, value, index, arrayref)

    /*base impl.*/ def baload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def bastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult =
        arraystore(pc, value, index, arrayref)

    /*base impl.*/ def caload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def castore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def daload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def dastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def faload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def fastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def iaload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def iastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def laload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult =
        arrayload(pc, index, arrayref)

    /*base impl.*/ def lastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def saload(
        pc:       Int,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def sastore(
        pc:       Int,
        value:    DomainValue,
        index:    DomainValue,
        arrayref: DomainValue
    ): ArrayStoreResult = arraystore(pc, value, index, arrayref)

}
