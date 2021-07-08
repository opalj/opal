package org.opalj.fpcf.fixtures.benchmark.known_types.multiple.types;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
//import edu.cmu.cs.glacier.qual.Immutable;

//@Immutable
@TransitivelyImmutableType("class is final and transitive immutable")
@TransitivelyImmutableClass("class is empty")
public final class FinalEmptyClassExtendsSuperClass1 extends SuperClass {
}
