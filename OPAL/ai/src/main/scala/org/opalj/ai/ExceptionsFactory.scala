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
package ai

/**
 * Defines factory methods for those exceptions that are (also) created by the JVM
 * when the evaluation of a specific bytecode instruction fails
 * (e.g., `idiv`, `checkcast`, `monitorexit`, `return`...).
 *
 * @author Michael Eichberg
 */
trait ExceptionsFactory extends ValuesDomain { domain ⇒

    /**
     * Creates a non-null object that represent a `Throwable` object and that has the
     * given `origin`.
     * If the `Throwable` was created by the VM while evaluating an instruction with
     * the program counter `pc` you should use the method [[ValueOriginForVMLevelValue]]
     * to translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def Throwable(origin: ValueOrigin): ExceptionValue

    final def VMThrowable(pc: Int): ExceptionValue = Throwable(ValueOriginForVMLevelValue(pc))

    /**
     * Creates a non-null object that represent a `ClassCastException` and that has the
     * given `origin`.
     * If the `ClassCastException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForVMLevelValue]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def ClassCastException(origin: ValueOrigin): ExceptionValue

    final def VMClassCastException(pc: Int): ExceptionValue = {
        ClassCastException(ValueOriginForVMLevelValue(pc))
    }

    def ClassNotFoundException(origin: ValueOrigin): ExceptionValue

    final def VMClassNotFoundException(pc: Int): ExceptionValue = {
        ClassNotFoundException(ValueOriginForVMLevelValue(pc))
    }

    /**
     * Creates a non-null object that represent a `NullPointerException` and that has the
     * given `origin`.
     * If the `NullPointerException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you should use the method [[ValueOriginForVMLevelValue]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def NullPointerException(origin: ValueOrigin): ExceptionValue

    final def VMNullPointerException(pc: Int): ExceptionValue = {
        NullPointerException(ValueOriginForVMLevelValue(pc))
    }

    /**
     * Creates a non-null object that represent an `IllegalMonitorStateException` and that has the
     * given `origin`.
     * If the `IllegalMonitorStateException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you should use the method [[ValueOriginForVMLevelValue]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def IllegalMonitorStateException(origin: ValueOrigin): ExceptionValue

    final def VMIllegalMonitorStateException(pc: Int): ExceptionValue = {
        IllegalMonitorStateException(ValueOriginForVMLevelValue(pc))
    }

    /**
     * Creates a non-null object that represent a `NegativeArraySizeException` and that has the
     * given `origin`.
     * If the `NegativeArraySizeException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForVMLevelValue]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def NegativeArraySizeException(origin: ValueOrigin): ExceptionValue

    final def VMNegativeArraySizeException(pc: Int): ExceptionValue = {
        NegativeArraySizeException(ValueOriginForVMLevelValue(pc))
    }

    /**
     * Creates a non-null object that represent a `ArrayIndexOutOfBoundsException` and that has the
     * given `origin`.
     * If the `ArrayIndexOutOfBoundsException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForVMLevelValue]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def ArrayIndexOutOfBoundsException(origin: ValueOrigin): ExceptionValue

    final def VMArrayIndexOutOfBoundsException(pc: Int): ExceptionValue = {
        ArrayIndexOutOfBoundsException(ValueOriginForVMLevelValue(pc))
    }

    /**
     * Creates a non-null object that represent a `ArrayStoreException` and that has the
     * given `origin`.
     * If the `ArrayStoreException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForVMLevelValue]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def ArrayStoreException(origin: ValueOrigin): ExceptionValue

    final def VMArrayStoreException(pc: Int): ExceptionValue = {
        ArrayStoreException(ValueOriginForVMLevelValue(pc))
    }

    /**
     * Creates a non-null object that represent a `ArithmeticException` and that has the
     * given `origin`.
     * If the `ArithmeticException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForVMLevelValue]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def ArithmeticException(origin: ValueOrigin): ExceptionValue

    final def VMArithmeticException(pc: Int): ExceptionValue = {
        ArithmeticException(ValueOriginForVMLevelValue(pc))
    }

}
