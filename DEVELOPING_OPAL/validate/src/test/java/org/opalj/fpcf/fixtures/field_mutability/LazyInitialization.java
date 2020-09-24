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
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;

/**
 * Test classes for simple lazy initialization patterns and anti-patterns.
 *
 * @author Dominik Helm
 */

class Simple {

    @ShallowImmutableField("Simple lazy initialization")
    @MutableField(value = "Analysis doesn't recognize lazy initialization",
            analyses = { L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class })
    private int x;

    public int init() {
        if (x == 0) {
            x = 5;
        }
        return x;
    }
}

class Local {

    @ShallowImmutableField("Lazy initialization with local")
    @MutableField(value = "Analysis doesn't recognize lazy initialization",
            analyses = { L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class })
    private int x;

    public int init() {
        int y = this.x;
        if (y == 0) {
            x = y = 5;
        }
        return y;
    }
}

class LocalWrong {

    @MutableField("Incorrect lazy initialization with local")
    private int x;

    public int init() {
        int y = this.x;
        if (y == 0) {
            x = 5;
        }
        return y;
    }
}

class LocalReversed {

    @ShallowImmutableField("Lazy initialization with local (reversed)")
    @MutableField(value = "Analysis doesn't recognize lazy initialization",
            analyses = { L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class })
    private int x;

    public int init() {
        int y = this.x;
        if (y == 0) {
            y = x = 5;
        }
        return y;
    }
}

class LocalReload {

    @ShallowImmutableField("Lazy initialization with local (reloading the field's value after the write)")
    @MutableField(value = "Analysis doesn't recognize lazy initialization",
            analyses = { L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class })
    private int x;

    public int init() {
        int y = this.x;
        if (y == 0) {
            x = 5;
            y = x;
        }
        return y;
    }
}

class SimpleReversed {

    @ShallowImmutableField("Simple lazy initialization (reversed)")
    @MutableField(value = "Analysis doesn't recognize lazy initialization",
            analyses = { L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class })
    private int x;

    public int init() {
        if (x != 0)
            return x;
        x = 5;
        return x;
    }
}

class SimpleWithDifferentDefault {

    @ShallowImmutableField(value = "Simple lazy initialization, but different default value",
            analyses = {})
    @MutableField(value = "Analysis doesn't recognize lazy initialization with different default")
    private int x;

    public SimpleWithDifferentDefault() {
        x = -1;
    }

    public SimpleWithDifferentDefault(int a) {
        this();
    }

    public int init() {
        if (x == -1) {
            x = 5;
        }
        return x;
    }
}

class WrongDefault {

    @MutableField("Not lazily initialized because of two different default values")
    private int x;

    public WrongDefault() {
    }

    public WrongDefault(int a) {
        this();
        x = -1;
    }

    public int init() {
        if (x == -1) {
            x = 5;
        }
        return x;
    }
}

class DeterministicCall {

    //FIXME: Iusse with Java11 @LazyInitialized("Lazy initialization with call to deterministic method")
    //FIXME: Issue with Java11 @NonFinal(value = "Analysis doesn't recognize lazy initialization",
    //       analyses = { L0FieldMutabilityAnalysis.class, L1FieldMutabilityAnalysis.class })
    private int x;

    public int init() {
        if (x == 0) {
            x = this.sum(5, 8);
        }
        return x;
    }

    private final int sum(int a, int b) {
        return a + b;
    }
}

class DeterministicCallWithParam {

    @MutableField("Lazy initialization is not the same for different invocations")
    private int x;

    public int init(int z) {
        if (x == 0) {
            x = this.sum(z, 8);
        }
        return x;
    }

    private final int sum(int a, int b) {
        return a + b;
    }
}

class DeterministicCallOnFinalField {

    @ShallowImmutableField("Lazy initialization with call to deterministic method on final field")
    @MutableField(value = "Analysis doesn't recognize lazy initialization",
            analyses = { L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class })
    private int x;

    @ShallowImmutableField("Declared final field")
    private final Inner inner;

    public DeterministicCallOnFinalField(int v) {
        inner = new Inner(v);
    }

    private final class Inner {

        final int val;

        public Inner(int v) {
            val = v;
        }

        public final int hashCode() {
            return val;
        }
    }

    public int init() {
        if (x == 0) {
            x = inner.hashCode();
        }
        return x;
    }
}

class DeterministicCallOnNonFinalField {

    @MutableField("Wrong lazy initialization with call to non-deterministic method on final field")
    private int x;

    @MutableField("Non final field")
    private Inner inner;

    public void createInner(int v) {
        inner = new Inner(v);
    }

    private final class Inner {

        final int val;

        public Inner(int v) {
            val = v;
        }

        public final int hashCode() {
            return val;
        }
    }

    public int init() {
        if (x == 0) {
            x = inner.hashCode();
        }
        return x;
    }
}

class NondeterministicCall {

    @MutableField("Wrong lazy initialization with call to non-deterministic method")
    private int x;

    private final Object object = new Object();

    public int init() {
        if (x == 0) {
            x = object.hashCode();
        }
        return x;
    }
}

class DoubleLocalAssignment {

    @ShallowImmutableField("Lazy initialization with a local that is updated twice")
    @MutableField(value = "Analysis doesn't recognize lazy initialization",
            analyses = { L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class })
    private int x;

    public int init() {
        if (x == 0) {
            int y = 5;
            y ^= -1;
            x = y;
        }
        return x;
    }
}

class DoubleAssignment {

    @MutableField("Field can be observed partially updated")
    private int x;

    public int init() {
        if (x == 0) {
            x = 5;
            x ^= -1;
        }
        return x;
    }
}

class VisibleInitialization {

    @MutableField("Incorrect because lazy initialization is visible")
    private int x;

    public int init() {
        int y = this.x;
        int z;
        if (y == 0) {
            y = x = 5;
            z = 3;
        } else {
            z = 2;
        }
        System.out.println(z);
        return y;
    }
}

class ExceptionInInitialization {

    /**
     * @note As the field write is dead, this field is really 'effectively final' as it will never
     * be different from the default value.
     */
    //FIXME: Issue with Java11 @EffectivelyFinal(value = "Field is never initialized, so it stays on its default value",
    //        analyses = { L1FieldMutabilityAnalysis.class, L2FieldMutabilityAnalysis.class })
    //FIXME: Issue with Java11 @NonFinal(value = "Instance field not considered by analysis",
    //        analyses = L0FieldMutabilityAnalysis.class)
    private int x;

    private int getZero() {
        return 0;
    }

    public int init() {
        int y = this.x;
        if (y == 0) {
            int z = 10 / getZero();
            y = x = 5;
        }
        return y;
    }
}

class PossibleExceptionInInitialization {

    @MutableField("Incorrect because lazy initialization is may not happen due to exception")
    private int x;

    public int init(int i) {
        int y = this.x;
        if (y == 0) {
            int z = 10 / i;
            y = x = 5;
        }
        return y;
    }
}

class CaughtExceptionInInitialization {

    @MutableField("Incorrect because lazy initialization is may not happen due to exception")
    private int x;

    public int init(int i) {
        int y = this.x;
        try {
            if (y == 0) {
                int z = 10 / i;
                y = x = 5;
            }
            return y;
        } catch (Exception e) {
            return 0;
        }
    }
}
