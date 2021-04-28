package org.opalj.fpcf.fixtures.benchmark.known_types.multiple;

import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.A;
import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.EmptyClassExtendsA1;
import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.EmptyClassExtendsA2;
import org.opalj.fpcf.properties.immutability.classes.TransitiveImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitiveImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the test case in which only two transitively immutable
 * well known objects are assigned to a field.
 */
@MutableType("class is not final")
@TransitiveImmutableClass("class has only one transitive immutable field")
public class DifferentObjectsAssigned {

    @TransitiveImmutableField("Field only refers to transitively immutable objects")
    @NonAssignableFieldReference("field is final")
    final A a;

    public DifferentObjectsAssigned(boolean b1){
        if(b1)
            this.a = new EmptyClassExtendsA1();
        else
            this.a = new EmptyClassExtendsA2();
    }
}



