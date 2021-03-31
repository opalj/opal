/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * This domain uses (only) the l1 domain related to handling type information.
 * I.e., this is the most basic domain that supports the tracking of precise type information;
 * however, neither Strings nor class values are tracked and also the nullness of values is not
 * refined after (an implicit) `NullPointerException`.
 *
 * @author Michael Eichberg
 */
class DefaultReferenceValuesDomain[Source](
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
    with l1.DefaultReferenceValuesBinding
    with l0.DefaultTypeLevelIntegerValues
    with l0.DefaultTypeLevelLongValues
    with l0.TypeLevelPrimitiveValuesConversions
    with l0.TypeLevelLongValuesShiftOperators

class DefaultReferenceValuesDomainWithCFGAndDefUse[Source](
        project: Project[Source],
        method:  Method
) extends DefaultReferenceValuesDomain(project, method)
    with RefineDefUseUsingOrigins

