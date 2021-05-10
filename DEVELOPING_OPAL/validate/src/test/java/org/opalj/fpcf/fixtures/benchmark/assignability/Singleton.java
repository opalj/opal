/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@MutableClass("")
public class Singleton {

    //@Immutable
    @MutableField("")
    @AssignableFieldReference("written by static initializer after the field becomes (indirectly) readable")
    private String name;

    private Singleton() {
        this.name = "";
    }

    // STATIC FUNCTIONALITY
    //@Immutable
    @NonTransitivelyImmutableField("")
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