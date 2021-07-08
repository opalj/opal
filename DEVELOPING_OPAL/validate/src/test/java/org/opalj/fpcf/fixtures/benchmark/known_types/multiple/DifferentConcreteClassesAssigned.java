package org.opalj.fpcf.fixtures.benchmark.known_types.multiple;

import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.SuperClass;
import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.FinalEmptyClassExtendsSuperClass1;
import org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types.FinalEmptyClassExtendsSuperClass2;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
//import edu.cmu.cs.glacier.qual.Immutable;
/**
 * This class represents the case in which only objects with well known transitively immutable class-types are assigned.
 */
//@Immutable
@MutableType("class is not final")
@TransitivelyImmutableClass("class has only one transitively immutable field")
public class DifferentConcreteClassesAssigned {

    //@Immutable
    @TransitivelyImmutableField("only two different objects with well known transitively immutable class types are assigned.")
    @NonAssignableField("field is final")
    final SuperClass a;

    DifferentConcreteClassesAssigned(Boolean b, FinalEmptyClassExtendsSuperClass1 fec1, FinalEmptyClassExtendsSuperClass2 fec2){
        if(b)
            this.a = fec1;
        else
            this.a = fec2;
        }

}
