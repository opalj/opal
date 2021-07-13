/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.general;

//import edu.cmu.cs.glacier.qual.Immutable;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

/**
 * This class represents the trivial case of a transitively immutable class with a transitively immutable type.
 */
//@Immutable
@TransitivelyImmutableType("class is final and as a result transitively immutable")
@TransitivelyImmutableClass("Class has no fields and, thus, it is transitively immutable")
public final class FinalClassWithNoFields {}