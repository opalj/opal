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
 * SPECIFIED LINE NUMBERS ARE STABLE.
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

    @InvokedConstructor(receiverType = SimpleBase.class, lineNumber = 70)
    @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 71)
    void callVirtualMethodInCatch() {
        try {
            throw new Exception();
        } catch (Exception e) {
            Base base = new SimpleBase();
            base.implementedMethod();
        }
    }

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 80)
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

    @InvokedConstructor(receiverType = SimpleBase.class, lineNumber = 111)
    @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 112)
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

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 123)
    void callStaticMethodInFinally() {
        try {
            throw new Exception();
        } catch (Exception e) {
            // empty
        } finally {
            SimpleBase.staticMethod();
        }
    }

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 132)
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

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 159)
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

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 173)
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

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 186)
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

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 199)
    void throwErrorCatchExceptionOrError() {
        try {
            throw new IllegalAccessError();
        } catch (Exception e) {
            AlternateBase.staticMethod();
        } catch (Error e) {
            SimpleBase.staticMethod();
        }
    }

    @InvokedMethod(receiverType = AlternateBase.class, name = "implementedMethod", lineNumber = 206)
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

    @InvokedMethod(receiverType = AlternateBase.class, name = "implementedMethod", lineNumber = 221)
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

    @InvokedMethod(receiverType = AlternateBase.class, name = "implementedMethod", lineNumber = 244)
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
            @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 256),
            @InvokedMethod(receiverType = AlternateBase.class, name = "implementedMethod", lineNumber = 258),
            @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 260) })
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
            @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 273),
            @InvokedMethod(receiverType = AlternateBase.class, name = "implementedMethod", lineNumber = 275) })
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
            @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 290),
            @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 291) })
    void possibleNullPointerException(Object o) {
        try {
            canThrowNullPointerException(o);
        } catch (NullPointerException e) {
            simple.implementedMethod();
            SimpleBase.staticMethod();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 302),
            @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 303) })
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

    // TODO more precise catch
}
