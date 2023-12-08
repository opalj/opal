/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.misc;

import callgraph.base.AlternateBase;
import callgraph.base.Base;
import callgraph.base.SimpleBase;

import org.opalj.ai.test.invokedynamic.annotations.InvokedConstructor;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethods;

/**
 * This class was used to create a class file with some well defined attributes. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * 
 * -->
 * 
 * @author Marco Jacobasch
 */
public class TryCatchFinally {

    Base simple = new SimpleBase();
    Base alternate = new AlternateBase();

    @InvokedConstructor(receiverType = "callgraph/base/SimpleBase", line = 70)
    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 71)
    void callVirtualMethodInCatch() {
        try {
            throw new Exception();
        } catch (Exception e) {
            Base base = new SimpleBase();
            base.implementedMethod();
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 80)
    void callStaticMethodInCatch() {
        try {
            throw new Exception();
        } catch (Exception e) {
            SimpleBase.staticMethod();
        }
    }

    void notCallVirtualMethodInCatch() {
        try {
            @SuppressWarnings("unused")
            int i = 0;
        } catch (Exception e) {
            Base base = new SimpleBase();
            base.implementedMethod();
        }
    }

    void notCallStaticMethodInCatch() {
        try {
            @SuppressWarnings("unused")
            int i = 0;
        } catch (Exception e) {
            SimpleBase.staticMethod();
        }
    }

    @InvokedConstructor(receiverType = "callgraph/base/SimpleBase", line = 111)
    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 112)
    void callVirtualMethodInFinally() {
        try {
            throw new Exception();
        } catch (Exception e) {
            // empty
        } finally {
            Base base = new SimpleBase();
            base.implementedMethod();
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 123)
    void callStaticMethodInFinally() {
        try {
            throw new Exception();
        } catch (Exception e) {
            // empty
        } finally {
            SimpleBase.staticMethod();
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 132)
    void callsMethodThatThrowsException() {
        try {
            throwsException();
        } catch (IllegalArgumentException e) {
            SimpleBase.staticMethod();
        } catch (Exception e) {
            // EMPTY
        }
    }

    void noCallInMethodThatThrowsExceptionCatchInfiniteLoop() {
        try {
            throwsException();
        } catch (IllegalArgumentException e) {
            while (!this.toString().isEmpty()) {
                // Infinite Loop
            }
            SimpleBase.staticMethod();
        } catch (Exception e) {
            // EMPTY
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 159)
    void callsMethodThatThrowsExceptionCatchNoInfiniteLoop() {
        try {
            throwsException();
        } catch (IllegalArgumentException e) {
            while (this.toString().isEmpty()) {
                // EMPTY
            }
            SimpleBase.staticMethod();
        } catch (Exception e) {
            // EMPTY
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 173)
    void callMethodInCatchWithDoWhile() {
        try {
            throw new IllegalAccessError();
        } catch (Exception e) {
            // EMPTY
        } catch (Error e) {
            do {
                SimpleBase.staticMethod();
            } while (false);
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 186)
    void callInCatchAfterDoWhileInfiniteLoop() {
        try {
            throw new IllegalAccessError();
        } catch (Exception e) {
            // EMPTY
        } catch (Error e) {
            do {
                SimpleBase.staticMethod();
            } while (this.getClass() == TryCatchFinally.class);
            AlternateBase.staticMethod();
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 199)
    void throwErrorCatchExceptionOrError() {
        try {
            throw new IllegalAccessError();
        } catch (Exception e) {
            AlternateBase.staticMethod();
        } catch (Error e) {
            SimpleBase.staticMethod();
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/AlternateBase", name = "implementedMethod", line = 206)
    void callInCatchWithBreak() {
        try {
            alternate.implementedMethod();
            throw new Exception();
        } catch (Exception e) {
            for (int i = 0; i < 2; i++) {
                if (i == 0) {
                    break;
                }
                SimpleBase.staticMethod();
            }
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/AlternateBase", name = "implementedMethod", line = 221)
    void noCallInCatchWithLabeledBreak() {
        try {
            alternate.implementedMethod();
            throw new Exception();
        } catch (Exception e) {
            label: for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    if (i == 0) {
                        break label;
                    }
                    SimpleBase.staticMethod();
                }

            }
        }
    }

    @InvokedMethod(receiverType = "callgraph/base/AlternateBase", name = "implementedMethod", line = 244)
    void callInEmptyForLoop() {
        try {
            throw new Exception();
        } catch (Exception e) {
            for (int i = 0; i < 0; i++) {
                simple.implementedMethod();
            }
            alternate.implementedMethod();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "createThrowable", line = 255),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 257),
            @InvokedMethod(receiverType = "callgraph/base/AlternateBase", name = "implementedMethod", line = 259),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 261) })
    void callMethodBasedOnThrowableType() {
        try {
            throw createThrowable();
        } catch (IllegalAccessException | RuntimeException e) {
            simple.implementedMethod();
        } catch (Exception e) {
            alternate.implementedMethod();
        } catch (Error e) {
            SimpleBase.staticMethod();
        } catch (Throwable e) {
            AlternateBase.staticMethod();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "throwThrowablePartly", line = 273),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 275),
            @InvokedMethod(receiverType = "callgraph/base/AlternateBase", name = "implementedMethod", line = 277) })
    void callMethodBasedOnThrowableTypePartly() {
        try {
            throwThrowablePartly();
        } catch (IllegalAccessException | RuntimeException e) {
            simple.implementedMethod();
        } catch (Exception e) {
            alternate.implementedMethod();
        } catch (Error e) {
            SimpleBase.staticMethod();
        } catch (Throwable e) {
            AlternateBase.staticMethod();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "canThrowNullPointerException", line = 291),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 293),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 294) })
    void possibleNullPointerException(Object o) {
        try {
            canThrowNullPointerException(o);
        } catch (NullPointerException e) {
            simple.implementedMethod();
            SimpleBase.staticMethod();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "canThrowNullPointerException", line = 304),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 306),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 307) })
    void possibleNullPointerExceptionCatchOtherExceptions(Object o) {
        try {
            canThrowNullPointerException(o);
        } catch (NullPointerException e) {
            simple.implementedMethod();
            SimpleBase.staticMethod();
        } catch (Exception e) {
            alternate.implementedMethod();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "alwaysThrowsCheckedException", line = 318),
            @InvokedMethod(receiverType = "callgraph/base/AlternateBase", name = "implementedMethod", isStatic = true, line = 320) })
    void callThrowCheckedException() {
        try {
            alwaysThrowsCheckedException();
        } catch (IllegalAccessException e) {
            alternate.implementedMethod();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "mayThrowException", line = 331),
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "alwaysThrowsException", line = 332),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 334),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 336) })
    void callMultipleMethodsInTry(Object o) {
        try {
            mayThrowException();
            alwaysThrowsException();
        } catch (IllegalArgumentException e) {
            simple.implementedMethod();
        } catch (RuntimeException e) {
            alternate.implementedMethod();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "alwaysThrowsException", line = 346),
            @InvokedMethod(receiverType = "callgraph/misc/TryCatchFinally", name = "mayThrowException", line = 347),
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 349) })
    void callMultipleMethodsInTryAlternateOrder(Object o) {
        try {
            alwaysThrowsException();
            mayThrowException();
        } catch (IllegalArgumentException e) {
            simple.implementedMethod();
        } catch (RuntimeException e) {
            alternate.implementedMethod();
        }
    }

    //
    //
    // Helper Functions after. To prevent changes of line numbers.
    //
    //
    void throwsException() {
        throw new IllegalArgumentException();
    }

    Throwable createThrowable() {
        int mod = this.hashCode() % 4;
        switch (mod) {
        case 0:
            return new IllegalArgumentException();
        case 1:
            return new IllegalAccessException();
        case 2:
            return new Exception();
        case 3:
            return new Error();
        default:
            return new Throwable();
        }
    }

    void throwThrowable() throws Throwable {
        throw createThrowable();
    }

    void throwThrowablePartly() throws Throwable {
        try {
            throw createThrowable();
        } catch (Error e) {
            // catch all Error
        }
    }

    void canThrowNullPointerException(Object o) {
        o.hashCode();
    }

    void mayThrowException() {
        if (this.hashCode() % 2 == 0)
            throw new IllegalArgumentException();
    }

    void mayThrowCheckedException() throws java.io.IOException {
        if (this.hashCode() % 2 == 0)
            throw new java.io.IOException();
    }

    void alwaysThrowsException() {
        throw new RuntimeException();
    }

    void alwaysThrowsCheckedException() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    // TODO more precise catch
}
