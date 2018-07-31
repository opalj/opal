/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import scala.collection.Set

import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.br.MethodSignature
import org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration
import org.opalj.ai.domain.DefaultHandlingOfMethodResults
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.DefaultDomainValueBinding
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.IgnoreSynchronization
import org.opalj.ai.domain.SpecialMethodsHandling
import org.opalj.ai.domain.l0
import org.opalj.ai.domain.l1
import org.opalj.ai.domain.la

/**
 * Domain object which can used to calculate the call graph using variable type analysis.
 * This domain uses advanced domains for tracking primitive values to rule out
 * potential dead branches/method calls on dead branches.
 *
 * @author Michael Eichberg
 */
class ExtVTACallGraphDomain[Source](
        val project:                      Project[Source],
        val fieldValueInformation:        FieldValueInformation,
        val methodReturnValueInformation: MethodReturnValueInformation,
        val cache:                        CallGraphCache[MethodSignature, Set[Method]],
        val method:                       Method
) extends CorrelationalDomain
    with DefaultDomainValueBinding
    with ThrowAllPotentialExceptionsConfiguration
    with TheProject
    with TheMethod
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with l1.DefaultIntegerRangeValues
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with l1.ConstraintsBetweenIntegerValues
    with l1.DefaultLongSetValues
    with l1.LongSetValuesShiftOperators
    with l1.ConcretePrimitiveValuesConversions
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l1.DefaultReferenceValuesBinding
    with l1.MaxArrayLengthRefinement
    with l1.NullPropertyRefinement
    with l0.TypeLevelInvokeInstructions // the foundation
    with la.RefinedTypeLevelInvokeInstructions
    with SpecialMethodsHandling
    // with l0.TypeLevelFieldAccessInstructions
    // Using the following domain reduces the number of call edges by ~4%
    with la.RefinedTypeLevelFieldAccessInstructions
