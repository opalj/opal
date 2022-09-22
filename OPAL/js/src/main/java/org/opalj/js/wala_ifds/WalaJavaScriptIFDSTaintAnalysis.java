package org.opalj.js.wala_ifds;

import com.ibm.wala.dataflow.IFDS.ICFGSupergraph;
import com.ibm.wala.dataflow.IFDS.PartiallyBalancedTabulationSolver;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSet;
import scala.Function3;

import java.util.function.Function;

public class WalaJavaScriptIFDSTaintAnalysis {

    private final ICFGSupergraph supergraph;

    private final JSDomain domain;

    private final Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sources;

    public WalaJavaScriptIFDSTaintAnalysis(CallGraph cg, Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sources)
    {
        supergraph = ICFGSupergraph.make(cg);
        this.sources = sources;
        this.domain = new JSDomain();
    }

    public TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<Integer,BasicBlockInContext<IExplodedBasicBlock>>> analyze() {
        PartiallyBalancedTabulationSolver<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>> solver = PartiallyBalancedTabulationSolver
                .createPartiallyBalancedTabulationSolver(new JSProblem(domain, supergraph, sources), null);
        TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>> result = null;
        try {
            result = solver.solve();
        } catch (CancelException e) {
            // this shouldn't happen
            assert false;
        }
        return result;
    }

    public static void startJSAnalysis(CallGraph CG, Function<BasicBlockInContext<IExplodedBasicBlock>, Boolean> sources,
                                        Function3<BasicBlockInContext<IExplodedBasicBlock>,
                                                  IntSet,
                                                  TabulationDomain<Pair<Integer,
                                                                        BasicBlockInContext<IExplodedBasicBlock>>,
                                                                   BasicBlockInContext<IExplodedBasicBlock>>,
                                                  Void> sinks) {
        WalaJavaScriptIFDSTaintAnalysis A = new WalaJavaScriptIFDSTaintAnalysis(CG, sources);

        TabulationResult<BasicBlockInContext<IExplodedBasicBlock>,
                         CGNode, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>> R = A.analyze();

        R.getSupergraphNodesReached().forEach((sbb) -> sinks.apply(sbb, R.getResult(sbb), A.domain));
    }
}
