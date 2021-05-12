/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;

public class Singleton {

    @MutableField(value = "written by static initializer after the field becomes (indirectly) readable",
            analyses = {L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference("written by static initializer after the field becomes (indirectly) readable")
    private String name;

    @NonTransitivelyImmutableField(
            value = "only initialized once by the constructor",
            analyses = { L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class ,
                    L0FieldImmutabilityAnalysis.class}
    )
    @MutableField(value = "instance field not recognized by analysis",
            analyses = L0FieldAssignabilityAnalysis.class)
    @NonAssignableFieldReference("only initialized once by the constructor")
    private Object mutex = new Object();

    private Singleton() {
        this.name = "";
    }

    public String getName() {
        synchronized (mutex) {
            return name;
        }
    }

    // STATIC FUNCTIONALITY
    @NonTransitivelyImmutableField(value = "only set in the static initializer", analyses = {L0FieldAssignabilityAnalysis.class,
    L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @NonAssignableFieldReference("only set in the static initializer")
    private static Singleton theInstance;

    static {
        theInstance = new Singleton();
        theInstance.name = "The Singleton Instance";
    }

    public static Singleton getInstance() {
        return theInstance;
    }

}
