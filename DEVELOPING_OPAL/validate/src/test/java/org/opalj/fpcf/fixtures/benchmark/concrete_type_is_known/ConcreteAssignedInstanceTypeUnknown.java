/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.concrete_type_is_known;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DeepImmutableType;
import org.opalj.fpcf.properties.immutability.types.ShallowImmutableType;

//@Immutable
@ShallowImmutableType("")
@ShallowImmutableClass("")
final class ConcreteAssignedInstanceTypeUnknown {

    //@Immutable
    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private Object object;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private C c;

    public ConcreteAssignedInstanceTypeUnknown(Object object, C c) {
        this.c = c;
        this.object = object;
    }
}

//@Immutable
@DeepImmutableType("Class is transitive immutable and final")
@DeepImmutableClass("Class C has no fields")
final class C{}