/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.general;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("The interface is extensible and, thus, has  a mutable type")
@TransitivelyImmutableClass("Since an interface has no state, it is deeply immutable.")
public interface Interface {

    void run();

    void stop();

    void reset();
}
