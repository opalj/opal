package org.opalj.xl.analyses.javascript.analyses.tajs;

import dk.brics.tajs.analysis.Analysis;

import dk.brics.tajs.analysis.Transfer;
import dk.brics.tajs.options.Options;
import dk.brics.tajs.solver.SolverSynchronizer;
import dk.brics.tajs.util.AnalysisException;

import static dk.brics.tajs.Main.run;

/**
 * Main class for the TAJS program analysis.
 */
public class TAJS {


    public void analyze(String filePath) {
        try {
            String[] args = {filePath};
            Analysis a = init(args, null);
            if (a == null)
                System.exit(-1);
            run(a);
            System.exit(0);
        } catch (AnalysisException e) {
            if (Options.get().isDebugOrTestEnabled()) {
                throw e;
            }
            //e.printStackTrace();
            System.exit(-2);
        }
    }

    public void resume(String filePath) {
        this.analyze(filePath);
    }

    public static Analysis init(String[] args, SolverSynchronizer sync) throws AnalysisException {
        return dk.brics.tajs.Main.init(args, sync, new Transfer());
    }

}

