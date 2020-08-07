package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
public class ClassWithStaticFields {

    @MutableFieldAnnotation("")
    @MutableReferenceAnnotation("")
    public static String name = "Class with static fields";

   // @ShallowImmutableFieldAnnotation("")
   // @ImmutableReferenceAnnotation("")
    private static int counter;
    ClassWithStaticFields(){
        counter++;
    }

    public void setCounter(int n) {counter =n;}

}
