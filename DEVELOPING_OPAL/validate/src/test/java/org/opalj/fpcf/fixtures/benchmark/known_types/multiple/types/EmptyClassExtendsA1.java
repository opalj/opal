package org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types;

import org.opalj.fpcf.properties.immutability.classes.TransitiveImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("class is not final")
@TransitiveImmutableClass("class is empty")
public class EmptyClassExtendsA1 extends A {
}
