/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * A domain that defines a common reference frame for all subsequent domains.
 */
trait SharedValuesDomain[Source]
    extends CorrelationalDomain
    with DefaultSpecialDomainValuesBinding
    with TheProject
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l0.TypeLevelDynamicLoads
    with l1.DefaultClassValuesBinding
    with l1.DefaultArrayValuesBinding
    with l1.DefaultIntegerRangeValues
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with l1.ConstraintsBetweenIntegerValues
    with l1.DefaultLongValues

/**
 * Domain that uses the l1 and l2 level ''stable'' domains.
 *
 * @note This domain is intended to be used for '''demo purposes only'''.
 *      '''Tests should create their own domains to make sure that
 *      the test results remain stable. The configuration of this
 *      domain just reflects a reasonable configuration that may
 *      change without further notice.'''
 *
 * @author Michael Eichberg
 */
class SharedDefaultDomain[Source](
        val project: Project[Source],
        val method:  Method
) extends TheMethod
    with ThrowAllPotentialExceptionsConfiguration
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with l0.TypeLevelFieldAccessInstructions
    with l0.TypeLevelInvokeInstructions
    with l0.TypeLevelDynamicLoads
    with SpecialMethodsHandling
    with SharedValuesDomain[Source]
    with l1.MaxArrayLengthRefinement // OPTIONAL
    with l1.NullPropertyRefinement // OPTIONAL
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with l1.ConstraintsBetweenIntegerValues
    with l1.LongValuesShiftOperators
    with l1.ConcretePrimitiveValuesConversions {

    override def toString: String = super.toString()+"("+method.toJava+")"

}
