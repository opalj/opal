package org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types;

import org.opalj.fpcf.properties.immutability.classes.TransitiveImmutableClass;
import org.opalj.fpcf.properties.immutability.types.TransitiveImmutableType;

@TransitiveImmutableType("class is final and transitive immutable")
@TransitiveImmutableClass("class is empty")
public final class FinalEmptyClassExtendsA2 extends A{
}
