package org.opalj.fpcf.fixtures.immutability.classes.interfaces;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
public interface TrivialInterfaceWithMethods {

public String getName();
public String getAge();
public void setName();
public void setAge();

}
