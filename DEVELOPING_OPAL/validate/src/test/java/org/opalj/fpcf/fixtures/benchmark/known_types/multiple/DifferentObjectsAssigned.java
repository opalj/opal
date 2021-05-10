package org.opalj.fpcf.fixtures.benchmark.known_types.multiple;

//import afu.org.checkerframework.checker.igj.qual.Mutable;
import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.A;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the test case in which only two transitively immutable
 * well known objects are assigned to a field.
 */
@MutableType("class is not final")
@NonTransitivelyImmutableClass("class has a non transitively immutable field but no mutable one")
public class DifferentObjectsAssigned {

    @TransitivelyImmutableField("Field only refers to transitively immutable objects")
    @NonAssignableFieldReference("field is final")
    final A a;

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("field is final")
    final A nonTransitivelyImmmutableField;

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

@TransitivelyImmutableClass("")
class E1 extends A{private int n = 8;}

@TransitivelyImmutableClass("")
class E2 extends A{private int n = 10;}

@MutableClass("")
class M extends A{public int n = 10;}


