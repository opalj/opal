/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;

/**
 * Simple demo class which updates the private field of another instance of this class.
 */
public class PrivateFieldUpdater {

    @NonTransitivelyImmutableField(
            value = "only initialized by the constructor",
            analyses = { L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class }
    )
    @MutableField(value = "instance field not recognized by analysis",
            analyses = L0FieldAssignabilityAnalysis.class)
    private String name;

    @MutableField("incremented whenever `this` object is passed to another `NonFinal` object")
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
