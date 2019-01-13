/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.br.fpcf.analyses.L0FieldMutabilityAnalysis;
import org.opalj.fpcf.properties.field_mutability.EffectivelyFinal;
import org.opalj.fpcf.properties.field_mutability.NonFinal;
import org.opalj.tac.fpcf.analyses.L1FieldMutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldMutabilityAnalysis;

public class Singleton {

    @NonFinal("written by static initializer after the field becomes (indirectly) readable")
    private String name;

    @EffectivelyFinal(
            value = "only initialized once by the constructor",
            analyses = { L1FieldMutabilityAnalysis.class, L2FieldMutabilityAnalysis.class }
    )
    @NonFinal(value = "instance field not recognized by analysis",
            analyses = L0FieldMutabilityAnalysis.class)
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

    @EffectivelyFinal("only set in the static initializer")
    private static Singleton theInstance;

    static {
        theInstance = new Singleton();
        theInstance.name = "The Singleton Instance";
    }

    public static Singleton getInstance() {
        return theInstance;
    }

}
