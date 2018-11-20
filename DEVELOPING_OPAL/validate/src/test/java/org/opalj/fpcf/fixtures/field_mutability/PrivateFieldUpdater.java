/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.fpcf.analyses.L1FieldMutabilityAnalysis;
import org.opalj.fpcf.analyses.L2FieldMutabilityAnalysis;
import org.opalj.fpcf.properties.field_mutability.EffectivelyFinal;
import org.opalj.fpcf.properties.field_mutability.NonFinal;

/**
 * Simple demo class which updates the private field of another instance of this class.
 */
public class PrivateFieldUpdater {

    @EffectivelyFinal(
            value = "only initialized by the constructor",
            analyses = { L1FieldMutabilityAnalysis.class, L2FieldMutabilityAnalysis.class }
            )
    private String name;

    @NonFinal("incremented whenever `this` object is passed to another `NonFinal` object")
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
