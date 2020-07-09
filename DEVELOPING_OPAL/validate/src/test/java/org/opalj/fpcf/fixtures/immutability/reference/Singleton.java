/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.reference;

import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceNotEscapesAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class Singleton {

    @MutableReferenceAnnotation("written by static initializer after the field becomes (indirectly) readable")
    private String name;

    @ImmutableReferenceNotEscapesAnnotation("only initialized once by the constructor")
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

    @ImmutableReferenceEscapesAnnotation("only set in the static initializer")
    private static Singleton theInstance;

    static {
        theInstance = new Singleton();
        theInstance.name = "The Singleton Instance";
    }

    public static Singleton getInstance() {
        return theInstance;
    }

}
