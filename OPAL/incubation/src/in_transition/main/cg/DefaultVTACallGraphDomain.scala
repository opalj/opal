/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import scala.collection.Set

import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.MethodSignature
import org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration
import org.opalj.ai.domain.DefaultHandlingOfMethodResults
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.DefaultSpecialDomainValuesBinding
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.IgnoreSynchronization
import org.opalj.ai.domain.l0
import org.opalj.ai.domain.l1
import org.opalj.ai.domain.la

/**
 * Domain object which is used to calculate the call graph using variable type analysis.
 *
 * @author Michael Eichberg
 */
class DefaultVTACallGraphDomain[Source](
        val project:                      Project[Source],
        val fieldValueInformation:        FieldValueInformation,
        val methodReturnValueInformation: MethodReturnValueInformation,
        val cache:                        CallGraphCache[MethodSignature, Set[Method]],
        val method:                       Method
) extends CorrelationalDomain
    with DefaultSpecialDomainValuesBinding
    with ThrowAllPotentialExceptionsConfiguration
    with TheProject
    with TheMethod
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with l0.DefaultTypeLevelLongValues
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l0.DefaultTypeLevelIntegerValues
    with l0.TypeLevelPrimitiveValuesConversions
    with l0.TypeLevelLongValuesShiftOperators
    with l1.DefaultReferenceValuesBinding
    with l0.TypeLevelInvokeInstructions
    with la.RefinedTypeLevelInvokeInstructions
    with la.RefinedTypeLevelFieldAccessInstructions
