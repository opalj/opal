/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("The interface is extensible and, thus, has  a mutable type")
@TransitivelyImmutableClass("Since an interface has no state, it is deeply immutable.")
public interface Interface {

    public void run();

    public void stop();

    public void reset();
}
