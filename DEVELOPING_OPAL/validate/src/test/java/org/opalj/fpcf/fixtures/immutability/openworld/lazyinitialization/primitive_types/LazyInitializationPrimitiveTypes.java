/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.lazyinitialization.primitive_types;

import org.opalj.fpcf.properties.immutability.field_assignability.*;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.tac.fpcf.analyses.FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.fieldassignability.L2FieldAssignabilityAnalysis;

/**
 * This class encompasses different cases of lazily initialized fields with primitive types without synchronization
 * but with determinism.
 *
 * @author Dominik Helm
 * @author Tobias Roth
 */
class Simple {

    @TransitivelyImmutableField(value = "field is lazily initialized and has primitive value", analyses = {})
    @MutableField(value = "The field is unsafely lazily initialized", analyses = { FieldImmutabilityAnalysis.class})
    @LazilyInitializedField(value = "Simple lazy initialization with primitive type", analyses = {})
    @UnsafelyLazilyInitializedField(value = "The analysis does not reconize determinism",
            analyses = {L2FieldAssignabilityAnalysis.class})
    private int x;

    public int init() {
        if (x == 0) {
            x = 5;
        }
        return x;
    }
}

class Local {

    @TransitivelyImmutableField(value = "field is lazily initialized and has primitive value", analyses = {})
    @LazilyInitializedField(value = "Lazy initialization with local", analyses = {})
    @UnsafelyLazilyInitializedField(value = "The analysis does not reconize determinism",
            analyses = {L2FieldAssignabilityAnalysis.class})
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

    @MutableField("Field is assignable")
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

    @TransitivelyImmutableField(value = "field is lazily initialized and has primitive value", analyses = {})
    @LazilyInitializedField(value = "Lazy initialization with local (reversed)", analyses = {})
    @UnsafelyLazilyInitializedField(value = "The analysis does not reconize determinism",
            analyses = {L2FieldAssignabilityAnalysis.class})
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

    @TransitivelyImmutableField(value = "field is lazily initialized and has primitive value", analyses = {})
    @LazilyInitializedField(value = "Simple lazy initialization (reversed)", analyses = {})
    @UnsafelyLazilyInitializedField(value = "The analysis cannot reconizes determinism",
            analyses = {L2FieldAssignabilityAnalysis.class})
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

    @TransitivelyImmutableField(value = "field is lazily initialized and has primitive value", analyses = {})
    @LazilyInitializedField(value = "Lazy initialization with call to deterministic method", analyses = {})
    @MutableField("field is unsafely lazily initialized and has primitive value")
    @UnsafelyLazilyInitializedField(value = "The analysis does not reconize determinism",
            analyses = {L2FieldAssignabilityAnalysis.class})
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

    @MutableField("field is unsafely lazily initialized and has primitive value")
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

    @TransitivelyImmutableField(value = "field is lazily initialized and has primitive value", analyses = {})
    @LazilyInitializedField(value = "Lazy initialization with call to deterministic method ", analyses = {})
    @UnsafelyLazilyInitializedField(value = "The analysis does not recognize determinism",
            analyses = {L2FieldAssignabilityAnalysis.class})
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

    //TODO @LazilyInitializedField("Despite the possible exception the field is always seen with one value")
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
