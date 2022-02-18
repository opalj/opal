/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.heros.analyses.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.heros.analyses.HerosAnalysis
import org.opalj.tac.fpcf.analyses.heros.cfg.OpalICFG
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact
import org.opalj.tac.fpcf.analyses.ifds.taint.NullFact

/**
 * A common subclass of all Heros taint analyses.
 *
 * @author Mario Trageser
 */
abstract class HerosTaintAnalysis(p: SomeProject, icfg: OpalICFG) extends HerosAnalysis[Fact](p, icfg) {

    /**
     * Uses the NullFact of the TaintAnalysis.
     */
    override def createZeroValue(): Fact = NullFact

}