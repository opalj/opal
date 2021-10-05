/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

/**
 * Defines factory methods for those exceptions that are (also) created by the JVM
 * when the evaluation of a specific bytecode instruction fails
 * (e.g., `idiv`, `checkcast`, `monitorexit`, `return`...).
 *
 * @author Michael Eichberg
 */
trait ExceptionsFactory extends ValuesDomain { domain =>

    /**
     * Creates a non-null object that represent a `Throwable` object and that has the
     * given `origin`.
     * If the `Throwable` was created by the VM while evaluating an instruction with
     * the program counter `pc` you should use the method [[ValueOriginForImmediateVMException]]
     * to translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def Throwable(origin: ValueOrigin): ExceptionValue

    final def VMThrowable(pc: Int): ExceptionValue = Throwable(ValueOriginForImmediateVMException(pc))

    /**
     * Creates a non-null object that represent a `ClassCastException` and that has the
     * given `origin`.
     * If the `ClassCastException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForImmediateVMException]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def ClassCastException(origin: ValueOrigin): ExceptionValue

    final def VMClassCastException(pc: Int): ExceptionValue = {
        ClassCastException(ValueOriginForImmediateVMException(pc))
    }

    def ClassNotFoundException(origin: ValueOrigin): ExceptionValue

    final def VMClassNotFoundException(pc: Int): ExceptionValue = {
        ClassNotFoundException(ValueOriginForImmediateVMException(pc))
    }

    /**
     * Creates a non-null object that represent a `NullPointerException` and that has the
     * given `origin`.
     * If the `NullPointerException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you should use the method [[ValueOriginForImmediateVMException]]
     * to translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def NullPointerException(origin: ValueOrigin): ExceptionValue

    final def VMNullPointerException(pc: Int): ExceptionValue = {
        NullPointerException(ValueOriginForImmediateVMException(pc))
    }

    final def MethodExternalNullPointerException(pc: Int): ExceptionValue = {
        NullPointerException(ValueOriginForMethodExternalException(pc))
    }

    /**
     * Creates a non-null object that represent an `IllegalMonitorStateException` and that has the
     * given `origin`.
     * If the `IllegalMonitorStateException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you should use the method [[ValueOriginForImmediateVMException]]
     * to translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def IllegalMonitorStateException(origin: ValueOrigin): ExceptionValue

    final def VMIllegalMonitorStateException(pc: Int): ExceptionValue = {
        IllegalMonitorStateException(ValueOriginForImmediateVMException(pc))
    }

    /**
     * Creates a non-null object that represent a `NegativeArraySizeException` and that has the
     * given `origin`.
     * If the `NegativeArraySizeException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForImmediateVMException]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def NegativeArraySizeException(origin: ValueOrigin): ExceptionValue

    final def VMNegativeArraySizeException(pc: Int): ExceptionValue = {
        NegativeArraySizeException(ValueOriginForImmediateVMException(pc))
    }

    /**
     * Creates a non-null object that represent a `ArrayIndexOutOfBoundsException` and that has the
     * given `origin`.
     * If the `ArrayIndexOutOfBoundsException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForImmediateVMException]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def ArrayIndexOutOfBoundsException(origin: ValueOrigin): ExceptionValue

    final def VMArrayIndexOutOfBoundsException(pc: Int): ExceptionValue = {
        ArrayIndexOutOfBoundsException(ValueOriginForImmediateVMException(pc))
    }

    /**
     * Creates a non-null object that represent a `ArrayStoreException` and that has the
     * given `origin`.
     * If the `ArrayStoreException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForImmediateVMException]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def ArrayStoreException(origin: ValueOrigin): ExceptionValue

    final def VMArrayStoreException(pc: Int): ExceptionValue = {
        ArrayStoreException(ValueOriginForImmediateVMException(pc))
    }

    /**
     * Creates a non-null object that represent a `ArithmeticException` and that has the
     * given `origin`.
     * If the `ArithmeticException` was created by the VM while evaluating an instruction
     * with the program counter `pc` you use the method [[ValueOriginForImmediateVMException]] to
     * translate that `pc` to the appropriate [[ValueOrigin]].
     */
    def ArithmeticException(origin: ValueOrigin): ExceptionValue

    final def VMArithmeticException(pc: Int): ExceptionValue = {
        ArithmeticException(ValueOriginForImmediateVMException(pc))
    }

}
