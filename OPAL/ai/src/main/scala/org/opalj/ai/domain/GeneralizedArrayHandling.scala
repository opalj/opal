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
package domain

/**
 * This (partial-)domain abstracts over the concrete methods for performing
 * array operations and provides an interface at a higher abstraction level.
 *
 * @author Michael Eichberg
 */
trait GeneralizedArrayHandling extends ReferenceValuesDomain { this: ValuesDomain ⇒

    //
    // NEW INTERFACE
    //

    /*abstract*/ def arrayload(
        pc: PC,
        index: DomainValue,
        arrayRef: DomainValue): ArrayLoadResult

    /*abstract*/ def arraystore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayRef: DomainValue): ArrayStoreResult

    //
    // IMPLEMENTATION OF DOMAIN'S "ARRAY METHODS"
    //

    /*base impl.*/ def aaload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def aastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult =
        arraystore(pc, value, index, arrayref)

    /*base impl.*/ def baload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def bastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult =
        arraystore(pc, value, index, arrayref)

    /*base impl.*/ def caload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def castore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def daload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def dastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def faload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def fastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def iaload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def iastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def laload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult =
        arrayload(pc, index, arrayref)

    /*base impl.*/ def lastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = arraystore(pc, value, index, arrayref)

    /*base impl.*/ def saload(
        pc: PC,
        index: DomainValue,
        arrayref: DomainValue): ArrayLoadResult = arrayload(pc, index, arrayref)

    /*base impl.*/ def sastore(
        pc: PC,
        value: DomainValue,
        index: DomainValue,
        arrayref: DomainValue): ArrayStoreResult = arraystore(pc, value, index, arrayref)

}
