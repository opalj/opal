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

import org.opalj.util.{ Answer, Yes, No, Unknown }

/**
 * Centralizes all configuration options related to how a domain should handle
 * situations in which the information about a value is not completely available and
 * which could lead to some kind of exception.
 *
 * Basically all domains that perform some kind of abstraction should mix in this trait
 * and query the respective method to decide if a respective exception should be thrown
 * if it is possible that an exception may be thrown.
 *
 * ==Usage==
 * If you need to adapt a setting just override the respective method in your domain.
 *
 * In general, the [[ThrowAllPotentialExceptionsConfiguration]] should be used as it
 * generates all exceptions that may be thrown.
 *
 * @author Michael Eichberg
 */
trait Configuration {

    /**
     * @return `true`
     */
    def methodThrowsAllCheckedExceptions: Boolean = true

    /**
     * If `true`, all instructions that may raise an arithmetic exception (e.g., ''idiv'',
     * ''ldiv'') should do so if it is impossible to statically determine that no
     * exception will occur. But, if we can statically determine that the operation will
     * raise an exception then the exception will be thrown – independently of this
     * setting. Furthermore, if we can statically determine that no exception will be
     * raised, no exception will be thrown. Hence, this setting only affects computations
     * with values with ''incomplete'' information.
     *
     * @return `true`
     */
    def throwArithmeticExceptions: Boolean = false

    /**
     * Returns `true` if potential `NullPointerExceptions` should be thrown and `false`
     * if such `NullPointerExceptions` should be ignored. However, if the interpreter
     * identifies a situation in which a `NullPointerException` is guaranteed to be
     * thrown, it will be thrown. Example:
     * {{{
     * def demo(o : Object) {
     *      o.toString  // - If "true", a NullPointerException will ALSO be thrown;
     *                  //   the operation also succeeds.
     *                  // - If "false" the operation will "just" succeed
     * }
     * }}}
     *
     * @return `true`
     */
    def throwNullPointerException: Boolean = false

    /**
     * @return `false` since it is extremely unlikely that this exception will ever
     *      be thrown. Basically, this exception can only be thrown if a compiler creates
     *      invalid bytecode.
     */
    def throwIllegalMonitorStateException: Boolean = false

    /**
     * @return `false`
     */
    def throwArrayIndexOutOfBoundsException: Boolean = false

    /**
     * @return `false`
     */
    def throwArrayStoreException: Boolean = false

    /**
     * @return `false`
     */
    def throwNegativeArraySizeException: Boolean = false

    /**
     * @return `false`
     */
    def throwClassCastException: Boolean = false

    /**
     * Throw a `ClassNotFoundException` if the a specific reference type is not
     * known in the current context. The context is typically a specific `Project`.
     *
     * @return `true`
     */
    def throwClassNotFoundException: Boolean = true

}
