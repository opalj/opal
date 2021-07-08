/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_transitively_immutable;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.benchmark.commons.CustomObject;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("Class has mutable fields  has a mutable state")
@MutableClass("It has a mutable state")
public class ClassWithAnArrayWithOneEscapingObject {

    //@Immutable
    @MutableField("Reference of the field is mutable")
    @AssignableField("Field is public")
    public CustomObject publicObject = new CustomObject();

    //@Immutable
    @NonTransitivelyImmutableField("Field is initialized with an Shallow immutable field")
    @EffectivelyNonAssignableField("Field is only initialized once.")
    private CustomObject[] arrayWithOneEscapingObject;

    public ClassWithAnArrayWithOneEscapingObject() {
        arrayWithOneEscapingObject = new CustomObject[]{publicObject};
    }

}
