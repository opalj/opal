package org.opalj.fpcf.fixtures.immutability.classes.interfaces;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("Not final interface")
@DeepImmutableClassAnnotation("Empty Interface")
public interface EmptyInterface {
}

@MutableTypeAnnotation("Not final Interface")
@DeepImmutableClassAnnotation("Interface")
interface TrivialInterfaceWithMethods {

    String getName();
    String getAge();
    void setName();
    void setAge();

}
