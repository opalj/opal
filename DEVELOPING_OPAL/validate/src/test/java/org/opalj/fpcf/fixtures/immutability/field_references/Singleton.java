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

public class Singleton {

    @MutableField(value = "written by static initializer after the field becomes (indirectly) readable",
            analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference("written by static initializer after the field becomes (indirectly) readable")
    private String name;

    @DeepImmutableField(value ="referenced object can not escape", analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(
            value = "only initialized once by the constructor",
            analyses = { L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class }
    )
    @MutableField(value = "instance field not recognized by analysis",
            analyses = L0FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference("only initialized once by the constructor")
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
    @ShallowImmutableField(value = "only set in the static initializer", analyses = {L0FieldImmutabilityAnalysis.class,
    L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference("only set in the static initializer")
    private static Singleton theInstance;

    static {
        theInstance = new Singleton();
        theInstance.name = "The Singleton Instance";
    }

    public static Singleton getInstance() {
        return theInstance;
    }

}
