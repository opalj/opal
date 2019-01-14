/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.br.fpcf.analyses.L0FieldMutabilityAnalysis;
import org.opalj.fpcf.properties.field_mutability.EffectivelyFinal;
import org.opalj.fpcf.properties.field_mutability.NonFinal;
import org.opalj.tac.fpcf.analyses.L1FieldMutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldMutabilityAnalysis;

/**
 * Simple demo class which updates the private field of another instance of this class.
 */
public class PrivateFieldUpdater {

    @EffectivelyFinal(
            value = "only initialized by the constructor",
            analyses = { L1FieldMutabilityAnalysis.class, L2FieldMutabilityAnalysis.class }
    )
    @NonFinal(value = "instance field not recognized by analysis",
            analyses = L0FieldMutabilityAnalysis.class)
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
