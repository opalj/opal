/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.known_types.multiple;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
class SuperClass {
}

@TransitivelyImmutableType("class is final and transitive immutable")
@TransitivelyImmutableClass("class is empty")
final class FinalEmptyClassExtendsSuperClass2 extends SuperClass {
}

@TransitivelyImmutableType("class is final and transitive immutable")
@TransitivelyImmutableClass("class is empty")
final class FinalEmptyClassExtendsSuperClass1 extends SuperClass {
}

@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
class EmptyClassExtendsSuperClass1 extends SuperClass {
}

@MutableType("class is not final")
@TransitivelyImmutableClass("class is empty")
class EmptyClassExtendsSuperClass2 extends SuperClass {}
