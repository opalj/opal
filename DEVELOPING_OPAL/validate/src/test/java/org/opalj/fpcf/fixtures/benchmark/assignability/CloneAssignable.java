/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("Class is extensible")
@MutableClass("Class has mutable field")
public class CloneAssignable {

    //@Immutable
    @MutableField("")
    @AssignableFieldReference("")
    int i;

    //@Immutable
    @MutableField("")
    @AssignableFieldReference("")
    CloneAssignable instance;

    public CloneAssignable clone(){
        CloneAssignable c = new CloneAssignable();
        c.i = 5;
        instance = c;
        return c;
    }
}
