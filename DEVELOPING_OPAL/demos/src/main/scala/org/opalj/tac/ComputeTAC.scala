/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse

/**
 * Shows how to get the 3-address code in the most efficient manner if it is required for
 * all methods and no property based computations should be carried out.
 *
 * @author Michael Eichberg
 */
object ComputeTAC {

    def main(args: Array[String]): Unit = {
        time {
            val p = Project(org.opalj.bytecode.JRELibraryFolder)
            p.updateProjectInformationKeyInitializationData(
                EagerDetachedTACAIKey,
                (oldFactory: Option[Method ⇒ Domain with RecordDefUse]) ⇒ {
                    if (oldFactory.isDefined) throw new IllegalStateException();
                    (m: Method) ⇒ new org.opalj.ai.domain.l0.PrimitiveTACAIDomain(p, m)
                }
            )
            p.get(EagerDetachedTACAIKey)
        } { t ⇒
            println("Loading the project and computing the tac for all methods took: "+t.toSeconds)
        }
    }
}
