/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.analyses.javascript
import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.analysis.axa.connector.IConnector
import dk.brics.tajs.analysis.nativeobjects.ECMAScriptObjects
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value
import org.opalj.xl.common.Constants
import org.opalj.xl.connector.AnalysisInterface

import java.io.File

object TAJS extends AnalysisInterface[Value, PKey.StringPKey, Value] {

    val PROTOCOL_NAME = "tajs-host-env"

    override def analyze(
        files:           List[File],
        propertyChanges: scala.collection.mutable.Map[PKey.StringPKey, Value],
        connector:       IConnector
    ): Value = {

        dk.brics.tajs.analysis.axa.connector.Connector.setConnector(connector)
        val args: Array[String] = (files.map(_.getPath) :+ "-flowgraph").toArray
        val analysis: Analysis = dk.brics.tajs.Main.init(args, null);
        val solver = analysis.getSolver()

        val mainFunction = solver.getFlowGraph.getMain
        val analysisLatticeElement = solver.getAnalysisLatticeElement
        val globalObject = ObjectLabel.make(ECMAScriptObjects.GLOBAL, ObjectLabel.Kind.OBJECT)

        mainFunction.getBlocks.forEach(block => {
            analysisLatticeElement
                .getStates(block)
                .values()
                .forEach(state => {
                    if (state.getStore.containsKey(globalObject)) {
                        propertyChanges.foreach(propertyChange => {
                            state.getStore.get(globalObject).setProperty(propertyChange._1, propertyChange._2)
                        })
                    }
                })
        })

        dk.brics.tajs.Main.run(analysis);

        val exitBB = mainFunction.getOrdinaryExit
        val states = solver.getAnalysisLatticeElement.getStates(exitBB)

        states.values().iterator().next().getStore.get(globalObject).getModified.get(Constants.jsrResultVariableName)
    }

}
