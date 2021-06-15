/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class encompasses possibilites in combination with field reads of the static analyzer.
 */
//@Immutable
@MutableType("Class is not final and has mutable field")
@MutableClass("Class has a mutable field")
public class Singleton {

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("Field is written by static initializer after the field becomes (indirectly) readable")
    private String name;

    private Singleton() {
        this.name = "";
    }

    // STATIC FUNCTIONALITY
    //@Immutable
    @NonTransitivelyImmutableField("The field is effectively non assignable but has a non transitively immutable type.")
    @EffectivelyNonAssignableField("The field is only set in the static initializer")
    private static Singleton theInstance;

    static {
        theInstance = new Singleton();
        theInstance.name = "The Singleton Instance";
    }

    public static Singleton getInstance() {
        return theInstance;
    }

}
