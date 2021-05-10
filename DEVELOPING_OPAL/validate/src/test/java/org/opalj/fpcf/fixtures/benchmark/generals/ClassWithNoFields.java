/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("Class is not final and thus extensible")
@TransitivelyImmutableClass("Class has no fields and, thus, no state")
public class ClassWithNoFields {
}
