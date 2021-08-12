/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet

/**
 * An andersen-style points-to analysis, i.e. points-to sets are modeled as subsets.
 * It uses [[TypeBasedPointsToSet]] as points-to sets, i.e. does not differentiate allocation sites for
 * the same types.
 * The analysis is field-based, array-based and context-insensitive.
 * As the analysis is build on top of the [[org.opalj.tac.TACAI]], it is (implicitly)
 * flow-sensitive (which is not the case for pure andersen-style).
 *
 * Points-to sets may be attached to the following entities:
 *  - [[org.opalj.tac.common.DefinitionSite]] for local variables.
 *  - [[org.opalj.br.Field]] for fields (either static of instance)
 *  - [[org.opalj.br.DeclaredMethod]] for the points-to set of the return values.
 *  - [[org.opalj.br.analyses.VirtualFormalParameter]] for the parameters of a method.
 *  - [[org.opalj.br.ObjectType]] for the element type of an array.
 *
 * @author Florian Kuebler
 */
class TypeBasedPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractPointsToAnalysis with TypeBasedAnalysis

object TypeBasedPointsToAnalysisScheduler extends AbstractPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => TypeBasedPointsToAnalysis =
        new TypeBasedPointsToAnalysis(_)
}
