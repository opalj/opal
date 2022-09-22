package org.opalj.js.wala_ifds;

import com.ibm.wala.dataflow.IFDS.*;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

class JSFlowFunctions implements IPartiallyBalancedFlowFunctions<BasicBlockInContext<IExplodedBasicBlock>> {
    ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph;
    JSDomain domain;

    public JSFlowFunctions(JSDomain domain, ISupergraph<BasicBlockInContext<IExplodedBasicBlock>, CGNode> supergraph)
    {
        this.supergraph = supergraph;
        this.domain = domain;
    }

    @Override
    public IUnaryFlowFunction getNormalFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
                                                    BasicBlockInContext<IExplodedBasicBlock> dest) {
        final IExplodedBasicBlock ebb = src.getDelegate();
        final IExplodedBasicBlock dbb = dest.getDelegate();

        return new IUnaryFlowFunction() {

            private void propagate(SSAInstruction inst, Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> vn, MutableIntSet r) {
                boolean propagates = inst instanceof SSAPhiInstruction || inst instanceof SSABinaryOpInstruction || inst instanceof SSAUnaryOpInstruction;

                if (propagates) {
                    for (int i = 0; i < inst.getNumberOfUses(); i++) {
                        if (vn.fst == inst.getUse(i)) {
                            Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> nvn = Pair.make(inst.getDef(), src);
                            if (! domain.hasMappedIndex(nvn)) {
                                domain.add(nvn);
                            }
                            r.add(domain.getMappedIndex(nvn));
                        }
                    }
                }
            }

            @Override
            public IntSet getTargets(int d1) {
                Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> vn = domain.getMappedObject(d1);
                MutableIntSet r = IntSetUtil.make(new int[] { domain.getMappedIndex(vn) });
                dbb.iteratePhis().forEachRemaining((inst) -> propagate(inst, vn, r));
                propagate(ebb.getInstruction(), vn, r);

                return r;
            }
        };
    }

    // taint parameters from arguments
    @Override
    public IUnaryFlowFunction getCallFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
                                                  BasicBlockInContext<IExplodedBasicBlock> dest, BasicBlockInContext<IExplodedBasicBlock> ret) {
        final IExplodedBasicBlock ebb = src.getDelegate();
        SSAInstruction inst = ebb.getInstruction();
        return (d1) -> {
            MutableIntSet r = IntSetUtil.make();
            Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> vn = domain.getMappedObject(d1);
            for (int i = 0; i < inst.getNumberOfUses(); i++) {
                if (vn.fst == inst.getUse(i)) {
                    Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> key = Pair.make(i+1, src);
                    if (! domain.hasMappedIndex(key)) {
                        domain.add(key);
                    }
                    r.add(domain.getMappedIndex(key));
                }
            }
            return r;
        };
    }

    // pass tainted values back to caller
    @Override
    public IUnaryFlowFunction getReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> call,
                                                    BasicBlockInContext<IExplodedBasicBlock> src, BasicBlockInContext<IExplodedBasicBlock> dest) {
        int retVal = call.getLastInstruction().getDef();
        return (d1) -> {
            Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> vn = domain.getMappedObject(d1);
            MutableIntSet result = IntSetUtil.make();
            supergraph.getPredNodes(src).forEachRemaining(pb -> {
                SSAInstruction inst = pb.getDelegate().getInstruction();
                if (inst instanceof SSAReturnInstruction && inst.getUse(0) == vn.fst) {
                    Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>> key = Pair.make(retVal, pb);
                    if (! domain.hasMappedIndex(key)) {
                        domain.add(key);
                    }
                    result.add(domain.getMappedIndex(key));
                }
            });
            return result;
        };
    }

    // local variables are not changed by calls.
    @Override
    public IUnaryFlowFunction getCallToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
                                                          BasicBlockInContext<IExplodedBasicBlock> dest) {
        return IdentityFlowFunction.identity();
    }

    // missing a callee, so assume it does nothing and keep local information.
    @Override
    public IUnaryFlowFunction getCallNoneToReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
                                                              BasicBlockInContext<IExplodedBasicBlock> dest) {
        return IdentityFlowFunction.identity();
    }

    // data flow back from an unknown call site, so just keep what comes back.
    @Override
    public IFlowFunction getUnbalancedReturnFlowFunction(BasicBlockInContext<IExplodedBasicBlock> src,
                                                         BasicBlockInContext<IExplodedBasicBlock> dest) {
        return IdentityFlowFunction.identity();
    }
}
