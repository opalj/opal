/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.open_world.generic;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the trivial case of a transitively immutable class with a mutable type.
 */
@MutableType("Class is not final and thus extensible")
@TransitivelyImmutableClass("Class has no fields and, thus, no state")
public class ClassWithNoFields {
}
