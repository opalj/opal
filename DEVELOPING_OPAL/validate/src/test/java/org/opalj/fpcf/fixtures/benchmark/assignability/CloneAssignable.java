/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@MutableClass("")
public class CloneAssignable {

    @MutableField("")
    @MutableFieldReference("")
    int i;

    @MutableField("")
    @MutableFieldReference("")
    CloneAssignable instance;

    public CloneAssignable clone(){
        CloneAssignable c = new CloneAssignable();
        c.i = 5;
        c.i = i;
        instance = c;
        c.i = 6;
        return c;
    }
}
