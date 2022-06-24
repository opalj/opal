/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.closed_world.known_types.multiple;

import org.opalj.fpcf.fixtures.immutability.closed_world.types.FinalEmptyClassExtendsSuperClass1;
import org.opalj.fpcf.fixtures.immutability.closed_world.types.FinalEmptyClassExtendsSuperClass2;
import org.opalj.fpcf.fixtures.immutability.closed_world.types.SuperClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the case in which only objects with well known transitively immutable class-types are assigned.
 */
@MutableType("class is not final")
@TransitivelyImmutableClass("class has only one transitively immutable field")
public class DifferentConcreteClassesAssigned {

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
