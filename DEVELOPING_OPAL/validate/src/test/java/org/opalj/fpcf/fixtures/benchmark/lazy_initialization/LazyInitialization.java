/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

/**
 * Test classes for simple lazy initialization patterns and anti-patterns regarding reference immutability analysis.
 *
 * @author Dominik Helm
 * @author Tobias Roth
 */

class Simple {

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

    @MutableField("")
    @MutableFieldReference(value = "Incorrect lazy initialization with local")
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

    @LazyInitializedNotThreadSafeFieldReference(value = "Lazy initialization with local (reversed)")
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

    @LazyInitializedNotThreadSafeFieldReference("Lazy initialization with local (reloading the field's value after the write)")
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

    @LazyInitializedNotThreadSafeFieldReference("Simple lazy initialization (reversed)")
    private int x;

    public int init() {
        if (x != 0)
            return x;
        x = 5;
        return x;
    }
}


class WrongDefault {

    @MutableField("")
    @MutableFieldReference(value = "Not lazily initialized because of two different default values")
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

    @LazyInitializedNotThreadSafeFieldReference(value = "Lazy initialization is not the same for different invocations")
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

    @LazyInitializedNotThreadSafeFieldReference(value = "Lazy initialization with call to deterministic method ")
    private int x;

    @ImmutableFieldReference(value = "Declared final field")
    private final Inner inner;

    public DeterministicCallOnFinalField(int v) {
        inner = new Inner(v);
    }

    private final class Inner {

        @DeepImmutableField(value="immutable reference with base type")
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

    @LazyInitializedNotThreadSafeFieldReference(value = "Wrong lazy initialization with call to non-deterministic method on final field")
    private int x;

    @MutableFieldReference("Non final field")
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

    @LazyInitializedNotThreadSafeFieldReference(value = "Wrong lazy initialization with call to non-deterministic method")
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
    @LazyInitializedNotThreadSafeFieldReference(value = "Lazy initialization with a local that is updated twice")
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

    @MutableFieldReference(value ="Field can be observed partially updated")
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

    @MutableFieldReference(value = "Incorrect because lazy initialization is visible")
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
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
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
    analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "Incorrect because lazy initialization is may not happen due to exception",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
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
