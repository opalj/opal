package org.opalj.js.wala_ifds;

import com.ibm.wala.dataflow.IFDS.PathEdge;
import com.ibm.wala.dataflow.IFDS.TabulationDomain;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.MutableMapping;

class JSDomain extends MutableMapping<Pair<Integer, BasicBlockInContext<IExplodedBasicBlock>>>
        implements TabulationDomain<Pair<Integer,BasicBlockInContext<IExplodedBasicBlock>>, BasicBlockInContext<IExplodedBasicBlock>>
{
    private static final long serialVersionUID = -1897766113586243833L;

    @Override
    public boolean hasPriorityOver(PathEdge<BasicBlockInContext<IExplodedBasicBlock>> pathEdge, PathEdge<BasicBlockInContext<IExplodedBasicBlock>> pathEdge1) {
        return false;
    }
}
