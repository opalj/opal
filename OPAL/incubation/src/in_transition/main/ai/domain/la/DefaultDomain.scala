/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package la

import scala.collection.Set

import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.br.MethodSignature
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.analyses.MethodReturnValuesKey
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.analyses.cg.CallGraphCache

/**
 * A default configuration of a Domain that uses - in particular - the domains implemented
 * in package `la`.
 *
 * @note This domain is intended to be used for '''demo purposes only'''.
 *      '''Tests should create their own domains to make sure that
 *      the test results remain stable. The configuration of this
 *      domain just reflects a reasonable configuration that may
 *      change without further notice.'''
 *
 * @author Michael Eichberg
 */
class DefaultDomain[Source](
        val project: Project[Source],
        val method:  Method
) extends CorrelationalDomain
    with domain.DefaultSpecialDomainValuesBinding
    with domain.ThrowAllPotentialExceptionsConfiguration
    with domain.l0.DefaultTypeLevelFloatValues
    with domain.l0.DefaultTypeLevelDoubleValues
    //with domain.l0.TypeLevelFieldAccessInstructions
    with RefinedTypeLevelFieldAccessInstructions
    with domain.l0.TypeLevelInvokeInstructions
    with RefinedTypeLevelInvokeInstructions
    //with domain.l1.DefaultReferenceValuesBinding
    with domain.l1.DefaultClassValuesBinding
    //with domain.l1.DefaultStringValuesBinding
    with domain.l1.NullPropertyRefinement
    with domain.l1.DefaultIntegerRangeValues
    with domain.l1.MaxArrayLengthRefinement
    //[CURRENTLY ONLY A WASTE OF RESOURCES] with domain.l1.ConstraintsBetweenIntegerValues
    // with domain.l1.DefaultIntegerSetValues
    with domain.l1.DefaultLongSetValues
    with domain.l1.LongSetValuesShiftOperators
    with domain.l1.ConcretePrimitiveValuesConversions
    with domain.DefaultHandlingOfMethodResults
    with domain.IgnoreSynchronization
    with domain.TheProject
    with domain.TheMethod
    // the following two are required to detect instructions that always throw
    // an exception (such as div by zero, a failing checkcast, a method call that
    // always fails etc.)
    with domain.RecordCFG
    with domain.l1.RecordAllThrownExceptions {

    override val maxCardinalityOfIntegerRanges: Long = 16L

    val fieldValueInformation: FieldValueInformation = project.get(FieldValuesKey)

    val methodReturnValueInformation: MethodReturnValueInformation = project.get(MethodReturnValuesKey)

    val cache: CallGraphCache[MethodSignature, Set[Method]] = {
        new CallGraphCache[MethodSignature, Set[Method]](project)
    }

}
