/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package translator

import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.analysis.nativeobjects.ECMAScriptObjects
import dk.brics.tajs.lattice.Context
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.PKey

package object translator {

    def getExitStatesAndContexts(analysis: Analysis) = {
        val mainFunction = analysis.getSolver.getFlowGraph.getMain
        val exitBlock = mainFunction.getOrdinaryExit
        val analysisLatticeElement = analysis.getSolver.getAnalysisLatticeElement
        val exitStates = analysisLatticeElement.getStates(exitBlock)
        exitStates
    }

    def globalObject: ObjectLabel[Context] = ObjectLabel.make(ECMAScriptObjects.GLOBAL, ObjectLabel.Kind.OBJECT).asInstanceOf[ObjectLabel[Context]]

    def tajsIdentifier(string: String): PKey.StringPKey = PKey.StringPKey.make(string)
}
