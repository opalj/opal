package org.opalj.fpcf.fixtures.immutability.type;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableClassAnnotation("")
@DeepImmutableTypeAnnotation("Class has no fields and is final")
public final class FinalEmptyClass {
}
