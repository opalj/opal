package org.opalj.fpcf.fixtures.immutability.closed_world.types;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
public class EmptyClassExtendsSuperClass2 extends SuperClass {
}
