package org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
public class EmptyClassExtendsA2 extends A {}

