/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.cg.xta

import org.opalj.fpcf.Entity
import org.opalj.br.ArrayType
import org.opalj.br.DefinedMethod
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Field

final class XTATypePropagationAnalysis private[analyses] (project: SomeProject)
    extends AbstractTypePropagationAnalysis(project) {

    override def getCorrespondingSetEntity(e: Entity): SetEntity = e match {
        case dm: DefinedMethod          ⇒ dm
        case vdm: VirtualDeclaredMethod ⇒ ExternalWorld
        case f: Field                   ⇒ f
        case ef: ExternalField          ⇒ ExternalWorld
        case at: ArrayType              ⇒ at
        case _                          ⇒ sys.error("unexpected entity")
    }
}

object XTATypePropagationAnalysisScheduler extends AbstractTypePropagationAnalysisScheduler {
    override def initializeAnalysis(project: SomeProject): AbstractTypePropagationAnalysis =
        new XTATypePropagationAnalysis(project)
}