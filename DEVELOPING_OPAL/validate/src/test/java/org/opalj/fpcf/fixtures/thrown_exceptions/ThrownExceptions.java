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

import org.opalj.fpcf.analyses.TransitiveThrownExceptionsAnalysis;
import org.opalj.fpcf.properties.thrown_exceptions.DoesNotThrowException;
import org.opalj.fpcf.properties.thrown_exceptions.MayThrowException;

/**
 * Test methods for the TransitiveThrownException analysis
 *
 * @author Andreas Muttscheller
 */
public class ThrownExceptions {

    @DoesNotThrowException("no call or explicit throw instruction")
    public static int staticDoesNotThrowException() {
        return 1;
    }

    @MayThrowException("will throw exception")
    public static int staticThrowsException() {
        throw new NullPointerException();
    }

    @DoesNotThrowException("no call or explicit throw instruction")
    public static int staticCallDoesNotThrowException() {
        staticCallDoesNotThrowException();
        return 1337;
    }

    @MayThrowException("will throw exception")
    public static int staticCallThrowsException() {
        staticThrowsException();
        return 42;
    }

    @DoesNotThrowException("no call or explicit throw instruction")
    public int doesNotThrowException() {
        return 2;
    }

    @MayThrowException("will throw exception")
    public int throwException() {
        throw new NullPointerException();
    }

    @DoesNotThrowException("no call or explicit throw instruction")
    public int callDoesNotThrowException() {
        return doesNotThrowException();
    }

    @MayThrowException("will throw exception")
    public int callThrowException() {
        return throwException();
    }

    @MayThrowException("will throw exception")
    public int simpleCycle(boolean b) {
        if (b) {
            simpleCycle(false);
        } else {
            throw new NullPointerException();
        }
        return 42;
    }

    @DoesNotThrowException("no call or explicit throw instruction")
    public int cycleA(boolean b) {
        if (b) {
            return cycleB();
        }
        return 42;
    }

    @DoesNotThrowException("no call or explicit throw instruction")
    public int cycleB() {
        cycleA(false);
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

    private static class FooBar extends Foo {
        @Override
        public int baz() {
            return 42;
        }

        @Override
        public int qux() {
            throw new NullPointerException();
        }
    }

    private Foo foo = new Foo();
    private FooBar foobar = new FooBar();

    @DoesNotThrowException("no call or explicit throw instruction")
    public int noSubclasses() {
        return foobar.baz();
    }

    @MayThrowException("will throw exception")
    public int subclassThrows() {
        return foo.baz();
    }

    @MayThrowException("will throw exception")
    public int superclassThrows() {
        return foo.qux();
    }
}
