/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.purity;

import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.properties.purity.CompileTimePure;
import org.opalj.fpcf.properties.purity.Pure;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;

/**
 * Base class for the `VirtualCalls` tests. Needs to be in separate file in order to be public.
 */
public class SubClassA extends BaseClass implements AnInterface {

    @CompileTimePure("Only returns result of exception-free computation on immutable parameter")
    @Pure(value = "Only returns result of exception-free computation on immutable parameter",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public int interfaceMethod(int i) {
        return i * 2;
    }

    @CompileTimePure("Only returns result of exception-free computation on immutable parameter")
    @Pure(value = "Only returns result of exception-free computation on immutable parameter",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public final int abstractMethod(int i) {
        return i + 2;
    }

    @CompileTimePure("Only returns result of exception-free computation on immutable parameter")
    @Pure(value = "Only returns result of exception-free computation on immutable parameter",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public int nonAbstractMethod(int i) {
        if (i > 0)
            return i;
        else
            return -1;
    }
}
