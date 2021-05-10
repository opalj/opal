package org.opalj.fpcf.fixtures.benchmark.known_types.multiple;

import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.A;
import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.FinalEmptyClassExtendsA1;
import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.FinalEmptyClassExtendsA2;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;

/**
 * This class represents the case in which only objects with well known transitively immutable class-types are assigned.
 */
//@MutableType("class is not final")
//@TransitivelyImmutableClass("class has only one transitive immutable field")
public class DifferentConcreteClassesAssigned {

    @TransitivelyImmutableField("only two different objects with well known transitive immutable class type are assigned.")
    @NonAssignableFieldReference("field is final")
    final A a;

    DifferentConcreteClassesAssigned(Boolean b, FinalEmptyClassExtendsA1 fec1, FinalEmptyClassExtendsA2 fec2){
        if(b)
            this.a = fec1;
        else
            this.a = fec2;
        }

}
