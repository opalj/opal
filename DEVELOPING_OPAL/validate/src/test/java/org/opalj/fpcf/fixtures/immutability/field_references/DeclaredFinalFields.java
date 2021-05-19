/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;

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

    @TransitivelyImmutableField(value = "Initialized directly", analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "Initialized directly", analyses = {L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField("Initialized directly")
    private final int a = 1;

    @TransitivelyImmutableField(value = "Initialized through instance initializer", analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "Initialized through instance initializer", analyses = {L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField("Initialized through instance initializer")
    private final int b;

    @TransitivelyImmutableField(value = "Initialized through constructor", analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "Initialized through constructor", analyses = {L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class})
    @EffectivelyNonAssignableField("Initialized through constructor")
    private final int c;

    @MutableField(value = "Prematurely read through super constructor", analyses = {L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class},
            prematurelyRead = true)
    @AssignableField(value = "Prematurely read through super constructor", prematurelyRead = true)
    private final int d;

    @MutableField(value = "Prematurely read through own constructor", analyses = {L0FieldAssignabilityAnalysis.class,
            L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class},
            prematurelyRead = true)
    @AssignableField(value = "Prematurely read through own constructor", prematurelyRead = true)
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
