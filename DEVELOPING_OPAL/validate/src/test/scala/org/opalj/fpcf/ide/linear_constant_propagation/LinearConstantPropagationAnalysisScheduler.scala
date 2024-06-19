/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.linear_constant_propagation

import scala.collection.immutable

import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationAnalysis
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.solver.JavaIDEAnalysis
import org.opalj.tac.fpcf.properties.TACAI

object LinearConstantPropagationAnalysisScheduler
    extends JavaIDEAnalysisScheduler[LinearConstantPropagationFact, LinearConstantPropagationValue] {
    override def property: JavaIDEPropertyMetaInformation[LinearConstantPropagationFact, LinearConstantPropagationValue] =
        LinearConstantPropagationPropertyMetaInformation

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeIteratorKey, PropertyStoreKey, RTACallGraphKey)

    override def init(
        project: SomeProject,
        ps:      PropertyStore
    ): JavaIDEAnalysis[LinearConstantPropagationFact, LinearConstantPropagationValue] = {
        new LinearConstantPropagationAnalysis(project)
    }

    override def uses: immutable.Set[PropertyBounds] =
        immutable.Set(PropertyBounds.finalP(TACAI), PropertyBounds.finalP(Callers))
}
