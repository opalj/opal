/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;

@ShallowImmutableClass("class has only the shallow immutable field tmc")
public class ClassWithShallowImmutableField {

    @ShallowImmutableField("field has an immutable field reference and mutable type")
    @ImmutableFieldReference("declared final reference")
    private final ClassWithPublicFields tmc = new ClassWithPublicFields();

    public ClassWithPublicFields getTmc() {
        return tmc;
    }
}
