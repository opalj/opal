package org.opalj.fpcf.fixtures.immutability.type;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_mutability.MutableType;

@MutableType("Class has no fields but is not final")
@DeepImmutableClassAnnotation("Class has no fields")
public class EmptyClass {
}
