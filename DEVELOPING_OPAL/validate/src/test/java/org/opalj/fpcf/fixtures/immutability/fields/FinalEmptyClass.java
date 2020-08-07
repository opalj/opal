package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("Because of deep immutable final class")
@DeepImmutableClassAnnotation("Because of Emptiness")
public final class FinalEmptyClass {
}
