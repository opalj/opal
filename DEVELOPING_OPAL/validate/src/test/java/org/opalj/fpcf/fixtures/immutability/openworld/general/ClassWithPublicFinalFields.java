/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.general;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("The type is extensible")
@TransitivelyImmutableClass("Class has no instance fields")
public class ClassWithPublicFinalFields {

    @NonTransitivelyImmutableField("Field is not assignable")
    @NonAssignableField("The field is public, final, and its value is only set once since it is static")
    public static final Object DEFAULT = new Object();

    public ClassWithPublicFinalFields() {
        System.out.println(ClassWithPublicFinalFields.DEFAULT);
    }
}
