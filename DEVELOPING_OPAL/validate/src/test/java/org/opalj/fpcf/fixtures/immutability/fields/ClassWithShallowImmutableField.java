/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;

@NonTransitivelyImmutableClass("class has only the shallow immutable field tmc")
public class ClassWithShallowImmutableField {

    @NonTransitivelyImmutableField("field has an immutable field reference and mutable type")
    @NonAssignableFieldReference("declared final reference")
    private final ClassWithPublicFields tmc = new ClassWithPublicFields();

    public ClassWithPublicFields getTmc() {
        return tmc;
    }
}
