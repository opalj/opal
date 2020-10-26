/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;

/**
 * Base class for tests below that calls a virtual method in its constructor that makes declared
 * final field visible in uninitialized state.
 *
 * @author Dominik Helm
 */
abstract class Super{

    public Super(){
        System.out.println(getD());
    }

    public abstract int getD();
}

/**
 * Tests for references that are declared final. Some of them are not strictly final because they can
 * be observed uninitialized.
 *
 * @author  Dominik Helm
 * @author Tobias Roth
 */
public class DeclaredFinalFields extends Super {

    @DeepImmutableField(value = "Initialized directly", analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "Initialized directly", analyses = {L0FieldImmutabilityAnalysis.class,
            L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference("Initialized directly")
    private final int a = 1;

    @DeepImmutableField(value = "Initialized through instance initializer", analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "Initialized through instance initializer", analyses = {L0FieldImmutabilityAnalysis.class,
            L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference("Initialized through instance initializer")
    private final int b;

    @DeepImmutableField(value = "Initialized through constructor", analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "Initialized through constructor", analyses = {L0FieldImmutabilityAnalysis.class,
            L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference("Initialized through constructor")
    private final int c;

    @MutableField(value = "Prematurely read through super constructor", analyses = {L0FieldImmutabilityAnalysis.class,
            L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class},
            prematurelyRead = true)
    @MutableFieldReference(value = "Prematurely read through super constructor", prematurelyRead = true)
    private final int d;

    @MutableField(value = "Prematurely read through own constructor", analyses = {L0FieldImmutabilityAnalysis.class,
            L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class},
            prematurelyRead = true)
    @MutableFieldReference(value = "Prematurely read through own constructor", prematurelyRead = true)
    private final int e;

    public DeclaredFinalFields() {
        super();
        c=1;
        d=1;
        System.out.println(getE());
        e=1;
    }

    public int getD(){
        return d;
    }

    public int getE(){
        return e;
    }

    // Instance initializer!
    {
        b = 1;
    }
}
