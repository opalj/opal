/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.lazy_initialization.primitive_types;

import org.opalj.fpcf.properties.immutability.field_assignability.*;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;

/**
 * Test classes for simple lazy initialization patterns and anti-patterns regarding reference immutability analysis.
 *
 * @author Dominik Helm
 * @author Tobias Roth
 */
class Simple {

    @TransitivelyImmutableField("field is lazily initialized and has primitive value")
    @LazilyInitializedField("Simple lazy initialization with primitive type")
    private int x;

    public int init() {
        if (x == 0) {
            x = 5;
        }
        return x;
    }
}

class Local {
    
    @TransitivelyImmutableField("field is lazily initialized and has primitive value")
    @LazilyInitializedField("Lazy initialization with local")
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

    @TransitivelyImmutableField("field is lazily initialized and has primitive value")
    @AssignableField("Incorrect lazy initialization with local")
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

    @TransitivelyImmutableField("field is lazily initialized and has primitive value")
    @LazilyInitializedField("Lazy initialization with local (reversed)")
    private int x;

    public int init() {
        int y = this.x;
        if (y == 0) {
            y = x = 5;
        }
        return y;
    }
}

class SimpleReversed {

    @TransitivelyImmutableField("field is lazily initialized and has primitive value")
    @LazilyInitializedField("Simple lazy initialization (reversed)")
    private int x;

    public int init() {
        if (x != 0)
            return x;
        x = 5;
        return x;
    }
}

class WrongDefault {

    @AssignableField("Not lazily initialized because of two different default values")
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

    @TransitivelyImmutableField("field is lazily initialized and has primitive value")
    @LazilyInitializedField("Lazy initialization with call to deterministic method")
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

    @TransitivelyImmutableField("field is lazily initialized and has primitive value")
    @UnsafelyLazilyInitializedField("Lazy initialization is not the same for different invocations")
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

    @TransitivelyImmutableField("field is lazily initialized and has primitive value")
    @LazilyInitializedField("Lazy initialization with call to deterministic method ")
    private int x;

    @NonAssignableField(value = "Declared final field")
    private final Inner inner;

    public DeterministicCallOnFinalField(int v) {
        inner = new Inner(v);
    }

    private final class Inner {

        @NonAssignableField("field is final")
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

    @UnsafelyLazilyInitializedField("Wrong lazy initialization with call to non-deterministic method on final field")
    private int x;

    @AssignableField("Non final field")
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

    @UnsafelyLazilyInitializedField("Wrong lazy initialization with call to non-deterministic method")
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

    @UnsafelyLazilyInitializedField("Lazy initialization with a local that is updated twice")
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

    @AssignableField("Field can be observed partially updated")
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

    @AssignableField("Incorrect because lazy initialization is visible")
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

class CaughtExceptionInInitialization {

    @LazilyInitializedField("Despite the possible exception the field is always seen with one value")
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
