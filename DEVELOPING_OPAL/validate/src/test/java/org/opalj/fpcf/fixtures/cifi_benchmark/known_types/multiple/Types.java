package org.opalj.fpcf.fixtures.cifi_benchmark.known_types.multiple;

//import edu.cmu.cs.glacier.qual.Immutable;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

//@Immutable
@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
class SuperClass {
}

//@Immutable
@TransitivelyImmutableType("class is final and transitive immutable")
@TransitivelyImmutableClass("class is empty")
final class FinalEmptyClassExtendsSuperClass2 extends SuperClass {
}

//@Immutable
@TransitivelyImmutableType("class is final and transitive immutable")
@TransitivelyImmutableClass("class is empty")
final class FinalEmptyClassExtendsSuperClass1 extends SuperClass {
}

//@Immutable
@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
class EmptyClassExtendsSuperClass1 extends SuperClass {
}

//@Immutable
@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
class EmptyClassExtendsSuperClass2 extends SuperClass {}