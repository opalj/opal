/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;

//@Immutable
@MutableClass("")
@DeepImmutableClass("As an interface has no state, it is deeply immutable.")
public interface Interface {

    public void run();

    public void stop();

    public void reset();
}
