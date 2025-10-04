/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.purity;

import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.properties.purity.CompileTimePure;
import org.opalj.fpcf.properties.purity.Pure;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;

/**
 * Base class for the `VirtualCalls` tests. Needs to be in separate file in order to be public.
 */
public abstract class BaseClass {

    // This method has pure (SubClassA) and side-effect free (SubClassB) implementations
    public abstract int abstractMethod(int i);

    // This (pure) method has an impure override in SubClassB
    @CompileTimePure("Only returns immutable parameter")
    @Pure(value = "Only returns immutable parameter",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public int nonAbstractMethod(int i) {
        return i;
    }

    @CompileTimePure("Only returns double of immutable parameter")
    @Pure(value = "Only returns double of immutable parameter",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public final int finalMethod(int i) {
        return i * 2;
    }
}
