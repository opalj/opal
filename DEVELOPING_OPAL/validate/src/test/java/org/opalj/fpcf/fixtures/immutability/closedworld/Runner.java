/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.closedworld;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

/**
 * This class represents the test case in which multiple objects are assigned to fields.
 * In a closed-world assumption the analysis is aware of the assigned objects' types.
 */

public class Runner {

    public static void main(String[] args){
        DifferentObjectAssigned doa = new DifferentObjectAssigned(true);
    }
}

@NonTransitivelyImmutableType("The class is non-transitively immutable and not inherited by a mutable class.")
@NonTransitivelyImmutableClass("The class has a non-transitively immutable field")
class DifferentObjectAssigned{

    @TransitivelyImmutableField("Field only refers to transitively immutable objects")
    @NonAssignableField("field is final")
    final SuperClass transitivelyImmutableField;

    @NonTransitivelyImmutableField("A mutable object is assigned to that field")
    @NonAssignableField("field is final")
    final SuperClass nonTransitivelyImmutableField;

    public DifferentObjectAssigned(Boolean b1){

        if(b1) {
            this.transitivelyImmutableField = new ImmutableClass1();
            this.nonTransitivelyImmutableField = new ImmutableClass1();
        }
        else {
            this.transitivelyImmutableField = new ImmutableClass2();
            this.nonTransitivelyImmutableField = new MutableClass();
        }
    }
}

@MutableType("The class is not inherited by a mutable class")
@TransitivelyImmutableClass("The class is empty")
class SuperClass {
}

@TransitivelyImmutableType("The class is transitively immutable and not inherited")
@TransitivelyImmutableClass("The class has only a transitively immutable field")
class ImmutableClass1 extends SuperClass {
    @TransitivelyImmutableField("")
    private int n = 8;
}

@TransitivelyImmutableType("The class is transitively immutable and not inherited")
@TransitivelyImmutableClass("The class has only a transitively immutable field")
class ImmutableClass2 extends SuperClass {
    @TransitivelyImmutableField("")
    private int n = 10;
}

@MutableType("The class is mutable")
@org.opalj.fpcf.properties.immutability.classes.MutableClass("The class has a mutable field")
class MutableClass extends SuperClass {

    @MutableField("The field is assignable and as a result mutable")
    @AssignableField("The field is public and as a result assignable")
    public int n = 10;
}
