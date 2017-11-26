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
package org.opalj.fpcf.fixtures.thrown_exceptions;

import org.opalj.fpcf.analyses.L1ThrownExceptionsAnalysis;
import org.opalj.fpcf.properties.thrown_exceptions.DoesNotThrowException;
import org.opalj.fpcf.properties.thrown_exceptions.ExpectedExceptions;

/**
 * Test methods for the thrown exceptions analysis.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
public class ExceptionUsages {

    //
    // CASES RELATED TO NOW EXCEPTIONS
    //

    @DoesNotThrowException(reason="just returns constant", requires={})
    public static int staticDoesNotThrowException() {
        return 1;
    }

    @DoesNotThrowException(reason="infinite self-recursive call", requires={})
    public static int staticCallDoesNotThrowException() {
        staticCallDoesNotThrowException();
        return 1337;
    }

    @DoesNotThrowException(reason="just returns constant", requires={})
    public int doesNotThrowException() {
        return 2;
    }

    @DoesNotThrowException(
            reason = "callee does not throw exception",
            requires = {L1ThrownExceptionsAnalysis.class}
    )
    public int callDoesNotThrowException() {
        return doesNotThrowException();
    }

    @DoesNotThrowException(
            reason = "self-recursive methods call (StackOverflows are generally ignored by OPAL)",
            requires = {}
    )
    public int selfRecursiveMethod(boolean b) {
        if (b) {
            return selfRecursiveMethod(false);
        } else {
            return 42;
        }
    }

    @DoesNotThrowException(
            reason = "mutual recursive method calls which throw no exception",
            requires = {}
    )
    public int cycleA(boolean b) {
        if (b) {
            return cycleB();
        }
        return 42;
    }

    @DoesNotThrowException(
            reason = "mutual recursive method calls which throw no exception",
            requires = {}
    )
    public int cycleB() {
        cycleA(false);
        return 42;
    }

    //
    // CASES RELATED TO EXCEPTIONS
    //

    @ExpectedExceptions()
    public static int staticThrowsException() {
        throw new NullPointerException();
    }


    @ExpectedExceptions()
    public static int staticCallThrowsException() {
        staticThrowsException();
        return 42;
    }


    @ExpectedExceptions()
    public int throwException() {
        throw new NullPointerException();
    }


    @ExpectedExceptions()
    public int callThrowException() {
        return throwException();
    }

    @ExpectedExceptions()
    public int simpleCycle(boolean b) {
        if (b) {
            simpleCycle(false);
        } else {
            throw new NullPointerException();
        }
        return 42;
    }



    // TODO Add tests for cycles, a->a and a->b->a


    private static class Foo {
        public int baz() {
            throw new NullPointerException();
        }

        public int qux() {
            return 42;
        }
    }

    private final static class FooBar extends Foo {
        @Override
        public int baz() {
            return 42;
        }

        @Override
        public int qux() {
            throw new NullPointerException();
        }
    }

    @DoesNotThrowException(
        reason="just calls empty default constructor and \"empty\" method of final class",
        requires={L1ThrownExceptionsAnalysis.class}
    )
    public int noSubclasses() {
        FooBar foobar = new FooBar();
        return foobar.baz();
    }

    private final Foo foo = new Foo();
    private final FooBar foobar = new FooBar();
    private final Foo fooBar = new FooBar();

    @ExpectedExceptions()
    public int subclassThrows() {
        return foo.baz();
    }

    @ExpectedExceptions()
    public int superclassThrows() {
        return foo.qux();
    }

    @ExpectedExceptions()
    public int superclassThrows2() {
        return fooBar.qux();
    }
}
