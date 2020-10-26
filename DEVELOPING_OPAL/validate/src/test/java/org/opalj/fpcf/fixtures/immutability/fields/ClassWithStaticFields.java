/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@DeepImmutableClass("")
public class ClassWithStaticFields {

    @MutableField("")
    @MutableFieldReference("")
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
