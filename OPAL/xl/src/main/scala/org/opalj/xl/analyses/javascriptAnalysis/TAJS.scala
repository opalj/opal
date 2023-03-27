/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package javascriptAnalysis

import org.opalj.xl.axa.frameworkconnector.AnalysisInterface
import dk.brics.tajs.analysis.Analysis
import dk.brics.tajs.Main.run
import dk.brics.tajs.analysis.Solver
import dk.brics.tajs.analysis.nativeobjects.ECMAScriptObjects
import dk.brics.tajs.flowgraph.Function
import dk.brics.tajs.lattice.Obj
import dk.brics.tajs.lattice.Renamings
import dk.brics.tajs.lattice.Value
import org.opalj.tac.common.DefinitionSite

import scala.collection.mutable
import dk.brics.tajs.lattice.ObjectLabel
import dk.brics.tajs.lattice.PKey

import java.io.File

object TAJS extends AnalysisInterface[Solver]{


  private final val language = "JavaScript"

  override def analyze(code: String, mappings: mutable.Map[String, Set[DefinitionSite]]): Solver = {
    val file: File = asFile(language, code)
    val args: Array[String] = Array(file.getPath, "-flowgraph", "-show-variable-info");
    val analysis: Analysis = dk.brics.tajs.Main.init(args, null);
    val solver = analysis.getSolver()
    val mainFunction:Function = solver.getFlowGraph.getMain
    val entryBlock = mainFunction.getEntry
    val analysisLatticeElement = solver.getAnalysisLatticeElement
    val states = analysisLatticeElement.getStates(entryBlock)

    val absentObject = Obj.makeAbsentModified().rename(new Renamings())
    mappings.foreach(mapping => {
      val name = mapping._1
      absentObject.setProperty(PKey.StringPKey.make(name), Value.makeAbsent())
    })

    val globalObject = ObjectLabel.make(ECMAScriptObjects.GLOBAL, ObjectLabel.Kind.OBJECT)
    val objectPrototypeNative = ObjectLabel.make(ECMAScriptObjects.OBJECT_PROTOTYPE, ObjectLabel.Kind.OBJECT)

    states.values().forEach(state=> {
      state.getStore.put(globalObject, absentObject)
      state.getStore.put(objectPrototypeNative, absentObject)
    })

    run(analysis);
    solver
  }

  override def resume(
      code: String,
      mappings: mutable.Map[String, Set[DefinitionSite]]
  ): Solver = {
    val file: File = asFile(language, code)
    this.analyze(file.getPath, mappings)
  }
}
