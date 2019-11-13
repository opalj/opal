package org.opalj.fpcf.fixtures.type_immutability;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("It has no fields and is final")
@DeepImmutableClassAnnotation("It has no fields and is final")
public final class FinalEmptyClass {

}
