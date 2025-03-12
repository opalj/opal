/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package integration

import scala.collection.immutable

import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.PropertyBounds
import org.opalj.ide.integration.IDEAnalysisScheduler
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ide.solver.JavaBackwardICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaForwardICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement
import org.opalj.tac.fpcf.properties.TACAI

/**
 * A base IDE analysis scheduler for Java programs.
 *
 * @author Robin Körkemeier
 */
abstract class JavaIDEAnalysisSchedulerBase[Fact <: IDEFact, Value <: IDEValue]
    extends IDEAnalysisScheduler[Fact, Value, JavaStatement, Method, JavaICFG] {
    /**
     * Key indicating which call graph should be used
     */
    val callGraphKey: CallGraphKey

    override def requiredProjectInformation: ProjectInformationKeys =
        super.requiredProjectInformation ++ Seq(
            DeclaredMethodsKey,
            TypeIteratorKey,
            callGraphKey
        )

    override def uses: Set[PropertyBounds] =
        super.uses.union(immutable.Set(
            PropertyBounds.finalP(TACAI),
            PropertyBounds.finalP(Callers)
        ))
}

object JavaIDEAnalysisSchedulerBase {
    /**
     * Trait to drop-in [[RTACallGraphKey]] as [[callGraphKey]]
     */
    trait RTACallGraph {
        val callGraphKey: CallGraphKey = RTACallGraphKey
    }

    /**
     * Trait to drop-in [[JavaForwardICFG]] for [[createICFG]]
     */
    trait ForwardICFG {
        def createICFG(project: SomeProject): JavaICFG = {
            new JavaForwardICFG(project)
        }
    }

    /**
     * Trait to drop-in [[JavaBackwardICFG]] for [[createICFG]]
     */
    trait BackwardICFG {
        def createICFG(project: SomeProject): JavaICFG = {
            new JavaBackwardICFG(project)
        }
    }
}
