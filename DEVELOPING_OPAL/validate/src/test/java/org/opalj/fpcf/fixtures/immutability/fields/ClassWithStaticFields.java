/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@TransitivelyImmutableClass("")
public class ClassWithStaticFields {

    @MutableField("")
    @AssignableFieldReference("")
    public static String name = "Class with static fields";

   // @ShallowImmutableFieldAnnotation("")
   // @ImmutableReferenceAnnotation("")
    private static int counter;
    ClassWithStaticFields() {
        counter++;
    }

    public void setCounter(int n) {
        counter = n;
    }
}
