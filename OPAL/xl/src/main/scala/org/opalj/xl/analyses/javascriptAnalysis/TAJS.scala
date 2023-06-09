/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javascriptAnalysis

import org.opalj.xl.axa.connector.AnalysisInterface
import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.analysis.Solver
import dk.brics.tajs.analysis.axa.connector.IConnector
import dk.brics.tajs.analysis.nativeobjects.ECMAScriptObjects
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.PKey
import dk.brics.tajs.lattice.Value

import java.io.File

object TAJS extends AnalysisInterface[Solver, PKey.StringPKey, Value] {

    val PROTOCOL_NAME = "tajs-host-env"

    var connector: IConnector = null
    override def analyze(
        file:            File,
        propertyChanges: scala.collection.mutable.Map[PKey.StringPKey, Value],
        connector:       IConnector
    ): Solver = {
        //-cp=/Users/tobiasroth/Documents/Projects/opal/OPAL/xl/src/main/scala/org/opalj/xl/analyses/javaAnalysis/bytecode/
        //-libcp=/opt/homebrew/Cellar/openjdk/19.0.2/libexec/openjdk.jdk/Contents/Home/jmods
        //-completelyLoadLibraries
        this.connector = connector
        dk.brics.tajs.analysis.axa.connector.Connector.setConnector(connector)

        val args: Array[String] = Array(file.getPath, "-flowgraph");
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
        println()
        solver
    }

    override def resume(
        file:            File,
        propertyChanges: scala.collection.mutable.Map[PKey.StringPKey, Value]
    ): Solver = {
        this.analyze(file, propertyChanges, connector)
    }
}
