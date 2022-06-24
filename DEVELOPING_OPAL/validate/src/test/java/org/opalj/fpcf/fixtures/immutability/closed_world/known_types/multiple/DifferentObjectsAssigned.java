/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.closed_world.known_types.multiple;

import org.opalj.fpcf.fixtures.immutability.closed_world.types.SuperClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the test case in which only two transitively immutable
 * well known objects are assigned to a field.
 */
@MutableType("class is not final")
@TransitivelyImmutableClass("class has only transitively immutable fields")
public class DifferentObjectsAssigned {

    @TransitivelyImmutableField("Field only refers to transitively immutable objects")
    @NonAssignableField("field is final")
    final SuperClass a;

    @TransitivelyImmutableField("")
    @NonAssignableField("field is final")
    final SuperClass nonTransitivelyImmmutableField;

    public DifferentObjectsAssigned(boolean b1){

       if(b1) {
            this.a = new E1(); //new EmptyClassExtendsA1();
            this.nonTransitivelyImmmutableField = new E1();
        }
        else {
            this.a = new E2();// new EmptyClassExtendsA2();
            this.nonTransitivelyImmmutableField = new M();
        }
    }
}

@TransitivelyImmutableClass("The class has only a transitively immutable field")
class E1 extends SuperClass {
    @TransitivelyImmutableField("")
    private int n = 8;
}

@TransitivelyImmutableClass("The class has only a transitively immutable field")
class E2 extends SuperClass {
    @TransitivelyImmutableField("")
    private int n = 10;
}

@MutableClass("The class has only a mutable field")
class M extends SuperClass {
    @AssignableField("")
    public int n = 10;
}
