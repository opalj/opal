package org.opalj.fpcf.fixtures.immutability.classes.inheriting;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@DeepImmutableClassAnnotation("Class has no fields but is not final")
public class EmptyClass {

}
