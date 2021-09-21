/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * Default configuration of a domain that uses the ''most capable'' `l1` domains
 *
 * @author Michael Eichberg
 */
class DefaultDomain[Source](
        val project: Project[Source],
        val method:  Method
) extends CorrelationalDomain
    with TheProject
    with TheMethod
    with DefaultSpecialDomainValuesBinding
    with ThrowAllPotentialExceptionsConfiguration
    with IgnoreSynchronization
    with l0.DefaultTypeLevelHandlingOfMethodResults
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l0.TypeLevelFieldAccessInstructions
    with l0.TypeLevelInvokeInstructions
    with l0.TypeLevelDynamicLoads
    with SpecialMethodsHandling
    // [NEEDED IF WE DON'T MIXIN CLASS AND STRING VALUES BINDING] with l1.DefaultReferenceValuesBinding
    // [NEEDED IF WE DON'T MIXIN CLASS VALUES BINDING] with l1.DefaultStringValuesBinding
    with l1.DefaultClassValuesBinding
    with l1.DefaultArrayValuesBinding // OPTIONAL (Tracks the length of arrays in common cases)
    with l1.MaxArrayLengthRefinement // OPTIONAL
    with l1.NullPropertyRefinement // OPTIONAL
    with l1.DefaultIntegerRangeValues
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with l1.ConstraintsBetweenIntegerValues
    with l1.DefaultLongValues
    with l1.LongValuesShiftOperators
    with l1.ConcretePrimitiveValuesConversions

/**
 * Configuration of a domain that uses the most capable `l1` domains and
 * which also records the abstract-interpretation time control flow graph.
 */
class DefaultDomainWithCFG[Source](
        project: Project[Source],
        method:  Method
) extends DefaultDomain[Source](project, method) with RecordCFG

/**
 * Configuration of a domain that uses the most capable `l1` domains and
 * which also records the abstract-interpretation time control flow graph and def/use
 * information.
 */
class DefaultDomainWithCFGAndDefUse[Source](
        project: Project[Source],
        method:  Method
) extends DefaultDomainWithCFG[Source](project, method)
    with RefineDefUseUsingOrigins
