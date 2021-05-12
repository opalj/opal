/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;

public class Singleton {

    @MutableField("written by static initializer after the field becomes (indirectly) readable")
    private String name;

    @NonTransitivelyImmutableField(
            value = "only initialized once by the constructor",
            analyses = { L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class }
    )
    @MutableField(value = "instance field not recognized by analysis",
            analyses = L0FieldAssignabilityAnalysis.class)
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

    @NonTransitivelyImmutableField("only set in the static initializer")
    private static Singleton theInstance;

    static {
        theInstance = new Singleton();
        theInstance.name = "The Singleton Instance";
    }

    public static Singleton getInstance() {
        return theInstance;
    }

}
