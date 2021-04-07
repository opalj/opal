/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * This domain uses the l1 level ''stable'' domains which can "only" represent single
 * values (basically just performs constant propagation).
 *
 * @author Michael Eichberg
 */
class DefaultSingletonValuesDomain[Source](
        val project: Project[Source],
        val method:  Method
) extends Domain
    with TypedValuesFactory
    with TheProject
    with TheMethod
    with DefaultSpecialDomainValuesBinding
    with ThrowAllPotentialExceptionsConfiguration
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l0.TypeLevelFieldAccessInstructions
    with l0.TypeLevelInvokeInstructions
    with l0.TypeLevelDynamicLoads
    with l0.DefaultReferenceValuesBinding
    with l1.DefaultIntegerValues
    with l1.DefaultLongValues
    with l1.LongValuesShiftOperators
    with l1.ConcretePrimitiveValuesConversions
