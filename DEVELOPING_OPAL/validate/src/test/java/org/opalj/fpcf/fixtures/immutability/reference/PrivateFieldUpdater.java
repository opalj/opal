/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.reference;

import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

/**
 * Simple demo class which updates the private field of another instance of this class.
 */
public class PrivateFieldUpdater {

    @ImmutableReferenceAnnotation("only initialized by the constructor")
    private String name;

    @MutableReferenceAnnotation("incremented whenever `this` object is passed to another `NonFinal` object")
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
