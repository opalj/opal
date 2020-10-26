/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;

public class Singleton {

    @MutableField("written by static initializer after the field becomes (indirectly) readable")
    private String name;

    @ShallowImmutableField(
            value = "only initialized once by the constructor",
            analyses = { L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class }
    )
    @MutableField(value = "instance field not recognized by analysis",
            analyses = L0FieldImmutabilityAnalysis.class)
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

    @ShallowImmutableField("only set in the static initializer")
    private static Singleton theInstance;

    static {
        theInstance = new Singleton();
        theInstance.name = "The Singleton Instance";
    }

    public static Singleton getInstance() {
        return theInstance;
    }

}
