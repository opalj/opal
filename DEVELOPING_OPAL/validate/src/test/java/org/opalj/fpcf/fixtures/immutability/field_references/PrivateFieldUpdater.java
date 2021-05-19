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
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

/**
 * Simple demo class which updates the private field of another instance of this class.
 */
public class PrivateFieldUpdater {
@TransitivelyImmutableField(value = "only initialized by the constructor", analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(
            value = "only initialized by the constructor",
            analyses = { L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class }
    )
    @EffectivelyNonAssignableField("only initialized by the constructor")
@MutableField(value = "instance field not recognized by analysis",
        analyses = L0FieldAssignabilityAnalysis.class)
    private String name;

    @MutableField(value = "incremented whenever `this` object is passed to another `NonFinal` object", analyses = {
            L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class,
            L0FieldImmutabilityAnalysis.class
    })
    @AssignableField(value = "incremented whenever `this` object is passed to another `NonFinal` object",
            analyses = {L3FieldAssignabilityAnalysis.class})
    private int i;

    private PrivateFieldUpdater(PrivateFieldUpdater s) {
        if (s != null) {
            s.i += 1;
            this.i = s.i;
            this.name = s.name + s.i;
        }
    }

    public String getName() {
        return name;
    }

    public int getI() {
        return i;
    }

}
