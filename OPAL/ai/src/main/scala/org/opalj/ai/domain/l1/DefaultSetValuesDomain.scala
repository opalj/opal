/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * This domain uses the l1 level ''stable'', partial domains that represent the
 * values of variables using sets.
 *
 * @author Michael Eichberg
 */
class DefaultSetValuesDomain[Source](
        val project: Project[Source],
        val method:  Method
) extends CorrelationalDomain
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
    // [NEEDED IF WE DON'T MIXIN CLASS AND STRING VALUES BINDING] with l1.DefaultReferenceValuesBinding
    // [NEEDED IF WE DON'T MIXIN CLASS VALUES BINDING] with l1.DefaultStringValuesBinding
    with l1.DefaultClassValuesBinding
    // [NOT YET SUFFICIENTLY TESTED:] with l1.DefaultArrayValuesBinding
    with l1.NullPropertyRefinement // OPTIONAL
    with l1.DefaultIntegerSetValues
    with l1.DefaultLongSetValues
    with l1.LongValuesShiftOperators
    with l1.ConcretePrimitiveValuesConversions
