package org.opalj.fpcf.fixtures.benchmark.generals;

import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;

public class ClassWithNonTransitivelyImmutableFields {

    //@Immutable
    @NonTransitivelyImmutableField("Final field with unknown class-type")
    @NonAssignableField("Declared final Field")
    private final Object finalObject;

    //@Immutable
    @NonTransitivelyImmutableField("Final field with ")
    @NonAssignableField("Declared final Field")
    private final ClassWithMutableFields cwmfConstructorAssigned;

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableField("Declared final Field")
    private final ClassWithMutableFields cwmf = new ClassWithMutableFields();

    //@Immutable
    @NonTransitivelyImmutableField("")
    @NonAssignableField("Declared final Field")
    private final ClassWithMutableFields fcwmf;

    //@Immutable
    @NonTransitivelyImmutableField("field has an immutable reference and mutable type")
    @NonAssignableField("Declared final Field")
    private final ClassWithNonTransitivelyImmutableFields cwpf2 =
            new ClassWithNonTransitivelyImmutableFields(new Object(), new ClassWithMutableFields(), new ClassWithMutableFields());

    //@Immutable
    @NonTransitivelyImmutableField("field has an immutable field reference and mutable type")
    @NonAssignableField("declared final reference")
    private final ClassWithNonTransitivelyImmutableFields cwpf =
            new ClassWithNonTransitivelyImmutableFields(new Object(), new ClassWithMutableFields(), new ClassWithMutableFields());

    public ClassWithNonTransitivelyImmutableFields getTmc() {
        return cwpf;
    }


    public ClassWithNonTransitivelyImmutableFields(Object o, ClassWithMutableFields cwmf, ClassWithMutableFields fcwmf){
        this.finalObject = o;
        this.cwmfConstructorAssigned = cwmf;
        this.fcwmf = fcwmf;
    }
}
