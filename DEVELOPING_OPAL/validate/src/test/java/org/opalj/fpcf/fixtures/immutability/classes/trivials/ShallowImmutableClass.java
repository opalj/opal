package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;
import org.opalj.fpcf.properties.type_mutability.MutableType;

@MutableTypeAnnotation("Because of not final class")
@DeepImmutableClassAnnotation("has shallow immutable field")
public class ShallowImmutableClass {
    @DeepImmutableFieldAnnotation("Because object can not escape")
    @ImmutableReferenceAnnotation("Because it is private")
    private MutableClass mutableClass = new MutableClass();
}

@MutableType("Because of not final mutable class")
@MutableClassAnnotation("Because of mutable field")
class MutableClass {
    @MutableFieldAnnotation("Because of mutable reference")
    @MutableReferenceAnnotation("Because of public field")
    public int n = 0;
}

@MutableTypeAnnotation("Because not final class")
@DeepImmutableClassAnnotation("Class has no fields but is not final")
class EmptyClass {
}

@DeepImmutableTypeAnnotation("Because of final deep immutable class")
@DeepImmutableClassAnnotation("Class has no fields and is final")
final class FinalEmptyClass {
}

@MutableTypeAnnotation("Because of not final class")
@DependentImmutableClassAnnotation("Das dependent immutable field")
class DependentImmutableClass<T> {
    @DependentImmutableFieldAnnotation(value = "Because of type T",genericString = "T")
    @ImmutableReferenceAnnotation("Private effectively final field")
    private T t;
    public DependentImmutableClass(T t){
        this.t = t;
    }
}
@MutableTypeAnnotation("Because of not final class")
@DeepImmutableClassAnnotation("Generic Type is not used as field Type")
class GenericTypeIsNotUsedAsFieldType<T> {
    private int n = 0;
    GenericTypeIsNotUsedAsFieldType(T t){
        String s = t.toString();
    }
}

@MutableTypeAnnotation("Because of mutable not final class")
@MutableClassAnnotation("Generic class but public field")
class ClassWithGenericPublicField<T> {
    @MutableFieldAnnotation("Because of mutable reference")
    @MutableReferenceAnnotation("Field is public")
    public T t;
    ClassWithGenericPublicField(T t){
        this.t = t;
    }
}
