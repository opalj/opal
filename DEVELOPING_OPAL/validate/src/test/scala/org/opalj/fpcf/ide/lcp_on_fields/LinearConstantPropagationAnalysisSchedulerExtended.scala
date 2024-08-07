/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ide.lcp_on_fields

import scala.collection.immutable

import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LCPOnFieldsPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.LinearConstantPropagationAnalysisExtended
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.solver.JavaIDEAnalysis
import org.opalj.tac.fpcf.properties.TACAI

object LinearConstantPropagationAnalysisSchedulerExtended
    extends JavaIDEAnalysisScheduler[LinearConstantPropagationFact, LinearConstantPropagationValue] {
    override def property: IDEPropertyMetaInformation[LinearConstantPropagationFact, LinearConstantPropagationValue] =
        LinearConstantPropagationPropertyMetaInformation

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, TypeIteratorKey, PropertyStoreKey, RTACallGraphKey)

    override def init(
        project: SomeProject,
        ps:      PropertyStore
    ): JavaIDEAnalysis[LinearConstantPropagationFact, LinearConstantPropagationValue] = {
        new LinearConstantPropagationAnalysisExtended(project)
    }

    override def uses: immutable.Set[PropertyBounds] =
        immutable.Set(
            PropertyBounds.finalP(TACAI),
            PropertyBounds.finalP(Callers),
            PropertyBounds.ub(LCPOnFieldsPropertyMetaInformation)
        )
}
