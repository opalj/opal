/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

/**
 * Test classes for simple lazy initialization patterns and anti-patterns regarding reference immutability analysis.
 *
 * @author Dominik Helm
 * @author Tobias Roth
 */

class Simple {

    /*@DeepImmutableField(value="Simple Lazy Initialization and primitive field type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "Simple lazy initialization", analyses = L2FieldImmutabilityAnalysis.class) */
    @MutableField(value = "Analysis doesn't recognize lazy initialization")
    @LazyInitializedNotThreadSafeFieldReference("Simple lazy initialization")
    private int x;

    public int init() {
        if (x == 0) {
            x = 5;
        }
        return x;
    }
}

class Local {

    /*@DeepImmutableField(value = "Lazy initialized and primitive type", analyses = {L3FieldImmutabilityAnalysis.class})
    @ShallowImmutableField(value = "Lazy initialization with local", analyses = {L2FieldImmutabilityAnalysis.class}) */
    @MutableField(value = "Analysis doesn't recognize lazy initialization")
    @LazyInitializedNotThreadSafeFieldReference("Lazy initialization with local")
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

    @MutableField(value = "Incorrect lazy initialization with local", analyses = {L0FieldAssignabilityAnalysis.class,
    L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference(value = "Incorrect lazy initialization with local",
            analyses = L0FieldImmutabilityAnalysis.class)
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

    /*@DeepImmutableField(value="Lazy initializatio with primitive type", analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "Lazy initialization with local (reversed)",
    analyses =  L2FieldImmutabilityAnalysis.class) */
    @MutableField(value = "Not thread safe lazy initialized field reference is seen as mutable")
    @LazyInitializedNotThreadSafeFieldReference(value = "Lazy initialization with local (reversed)",
            analyses = L3FieldAssignabilityAnalysis.class)
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

    //@DeepImmutableField(value = "Lazy initialization with primitive type",
    //analyses =  L3FieldImmutabilityAnalysis.class)
    //@ShallowImmutableField(value = "Lazy initialization with local (reloading the field's value after the write)",
    //analyses = L2FieldImmutabilityAnalysis.class)
    @MutableField(value = "Analysis doesn't recognize lazy initialization",
            analyses = { L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class ,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class })
    @LazyInitializedNotThreadSafeFieldReference(
            value = "Lazy initialization with local (reloading the field's value after the write)")
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

   /* @DeepImmutableField(value = "Lazy initialization with primitive type",
    analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "Simple lazy initialization (reversed)",
    analyses = L2FieldImmutabilityAnalysis.class) */
    @MutableField(value = "Analysis doesn't recognize lazy initialization")
    @LazyInitializedNotThreadSafeFieldReference(value = "Simple lazy initialization (reversed)",
            analyses = L3FieldAssignabilityAnalysis.class)
    private int x;

    public int init() {
        if (x != 0)
            return x;
        x = 5;
        return x;
    }
}

class SimpleWithDifferentDefault {

    @NonTransitivelyImmutableField(value = "Simple lazy initialization, but different default value",
            analyses = {})
    @MutableField(value = "Analysis doesn't recognize lazy initialization with different default", analyses =
            {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference(value = "Analysis doesn't recognize lazy initialization with different default",
            analyses = L3FieldAssignabilityAnalysis.class)
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

    @MutableField(value = "Not lazily initialized because of two different default values",
    analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference(value = "Not lazily initialized because of two different default values",
            analyses = L3FieldAssignabilityAnalysis.class)
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
    @LazyInitializedNotThreadSafeFieldReference("Lazy initialization with call to deterministic method")
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

    @MutableField(value = "Lazy initialization is not the same for different invocations",
    analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @LazyInitializedNotThreadSafeFieldReference(value = "Lazy initialization is not the same for different invocations",
            analyses = L3FieldAssignabilityAnalysis.class)
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

    /*@DeepImmutableField(value = "Lazy initialization with call to deterministic " +
            "method on final field with primitive type", analyses =  L3FieldImmutabilityAnalysis.class
    )
    @ShallowImmutableField(value = "Lazy initialization with call to deterministic method on final field",
    analyses = L2FieldImmutabilityAnalysis.class) */
    @MutableField(value="Not thread safe lazy initialized field reference is seen as mutable")
    @LazyInitializedNotThreadSafeFieldReference(value = "Lazy initialization with call to deterministic method " +
            "on final field", analyses = L3FieldAssignabilityAnalysis.class)
    private int x;

    @NonTransitivelyImmutableField(value = "Declared final field",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @NonAssignableFieldReference(value = "Declared final field", analyses = L3FieldAssignabilityAnalysis.class)
    private final Inner inner;

    public DeterministicCallOnFinalField(int v) {
        inner = new Inner(v);
    }

    private final class Inner {

        @TransitivelyImmutableField(value="immutable reference with base type",
                analyses = L0FieldImmutabilityAnalysis.class)
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

    @MutableField(value = "Wrong lazy initialization with call to non-deterministic method on final field",
    analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @LazyInitializedNotThreadSafeFieldReference(value = "Wrong lazy initialization with call to non-deterministic method on final field",
            analyses = L0FieldImmutabilityAnalysis.class)
    private int x;


    @MutableField(value = "Non final field",
    analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class,
    L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference("Non final field")
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

    @MutableField(value = "Wrong lazy initialization with call to non-deterministic method",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @LazyInitializedNotThreadSafeFieldReference(value = "Wrong lazy initialization with call to non-deterministic method",
    analyses = L3FieldAssignabilityAnalysis.class)
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
    /*@DeepImmutableField(value = "Lazy initialization with a local that is updated twice and primitive type",
            analyses =  L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "Lazy initialization with a local that is updated twice",
    analyses =  L2FieldImmutabilityAnalysis.class) */
    @MutableField(value = "Analysis doesn't recognize lazy initialization")
    @LazyInitializedNotThreadSafeFieldReference(value = "Lazy initialization with a local that is updated twice",
            analyses = L3FieldAssignabilityAnalysis.class)
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
    @MutableField(value = "Field can be observed partially updated", analyses = {L0FieldAssignabilityAnalysis.class,
    L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference(value ="Field can be observed partially updated",
            analyses = L3FieldAssignabilityAnalysis.class)
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

    @MutableField(value= "Incorrect because lazy initialization is visible", analyses = {L0FieldAssignabilityAnalysis.class,
    L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference(value = "Incorrect because lazy initialization is visible",
            analyses = L3FieldAssignabilityAnalysis.class)
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
    @LazyInitializedNotThreadSafeFieldReference(value = "L1 Domain can not recognize this exception", //Field is never initialized, so it stays on its default value",
            analyses = L3FieldAssignabilityAnalysis.class)
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



class CaughtExceptionInInitialization {
    //TODO reasoning
    @MutableField(value = "Incorrect because lazy initialization is may not happen due to exception",
    analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference(value = "Incorrect because lazy initialization is may not happen due to exception",
            analyses = L3FieldAssignabilityAnalysis.class)
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
