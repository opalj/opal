/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("Class has no fields and, thus, no state")
public class EmptyClass {
}
