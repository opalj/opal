package org.opalj.fpcf.fixtures.immutability.classes.trivials;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("Because of final deep immutable class")
@DeepImmutableClassAnnotation("Class has no fields and is final")
public final class FinalEmptyClass {
}
