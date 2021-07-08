package org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;
//import edu.cmu.cs.glacier.qual.Immutable;

//@Immutable
@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
public class EmptyClassExtendsSuperClass1 extends SuperClass {
}
