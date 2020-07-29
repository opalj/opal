package org.opalj.fpcf.fixtures.immutability.classes.inheriting;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;
import org.opalj.fpcf.properties.type_mutability.MutableType;

@MutableTypeAnnotation("Not final class")
@DeepImmutableClassAnnotation("Class has no fields but is not final")
public class EmptyClass {

}

@MutableTypeAnnotation("Not final class")
@DeepImmutableClassAnnotation("empty class inheriting empty class")
class EmptyClassInheritingEmptyClass extends EmptyClass{
}

@MutableType("Because of mutable class")
@MutableClassAnnotation("Because of extending mutable class")
final class EmptyClassInheritingMutableClass extends MutableClass {

}

@MutableTypeAnnotation("Because of mutable class")
@MutableClassAnnotation("Because of mutable field")
class MutableClass {
    @MutableFieldAnnotation("Mutable reference")
    @MutableReferenceAnnotation("public field")
    public int n= 0;
}