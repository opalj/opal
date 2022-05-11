/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac

import java.io.File

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.analyses.Project
import org.opalj.br.Method

/**
 * Shows how to get the 3-address code in the most efficient manner if it is required for
 * all methods and no property based computations should be carried out.
 *
 * @author Michael Eichberg
 */
object ComputeTAC {

    def main(args: Array[String]): Unit = {
        val rootFolder =
            if (args.isEmpty)
                org.opalj.bytecode.JRELibraryFolder
            else
                new File(args(0))

        val tacProvider = time {
            val p = Project(rootFolder)
            p.updateProjectInformationKeyInitializationData(EagerDetachedTACAIKey) { oldFactory =>
                if (oldFactory.isDefined) throw new IllegalStateException();
                (m: Method) => new org.opalj.ai.domain.l0.PrimitiveTACAIDomain(p, m)
            }
            p.get(EagerDetachedTACAIKey)
        } { t =>
            println("Loading the project and computing the tac for all methods took: "+t.toSeconds)
        }

        // Now, you can use the TACProvider to get the TAC for a specific method.
        println(tacProvider.asInstanceOf[scala.collection.Map[_, _]].size)
    }
}
