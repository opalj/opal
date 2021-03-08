/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.concrete_class_type_is_known;

import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.ShallowImmutableType;

@ShallowImmutableType("")
@ShallowImmutableClass("")
final class ConcreteAssignedInstanceTypeUnknown {

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private Object object;

    public ConcreteAssignedInstanceTypeUnknown(Object object) {
        this.object = object;
    }
}