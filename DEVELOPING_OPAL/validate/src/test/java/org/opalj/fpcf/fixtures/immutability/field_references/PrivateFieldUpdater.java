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
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

/**
 * Simple demo class which updates the private field of another instance of this class.
 */
public class PrivateFieldUpdater {
@DeepImmutableField(value = "only initialized by the constructor", analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(
            value = "only initialized by the constructor",
            analyses = { L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class }
    )
    @ImmutableFieldReference("only initialized by the constructor")
@MutableField(value = "instance field not recognized by analysis",
        analyses = L0FieldImmutabilityAnalysis.class)
    private String name;

    @MutableField(value = "incremented whenever `this` object is passed to another `NonFinal` object", analyses = {
            L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class,
            L3FieldImmutabilityAnalysis.class
    })
    @MutableFieldReference(value = "incremented whenever `this` object is passed to another `NonFinal` object",
            analyses = {L0FieldReferenceImmutabilityAnalysis.class})
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
