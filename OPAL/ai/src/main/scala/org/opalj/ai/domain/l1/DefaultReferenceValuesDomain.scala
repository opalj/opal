/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.opalj.br.Method
import org.opalj.br.analyses.Project

/**
 * This domain uses the l1 level ''stable'', partial domains.
 *
 * @author Michael Eichberg
 */
class DefaultReferenceValuesDomain[Source](
        val project: Project[Source],
        val method:  Method
) extends CorrelationalDomain
    with TheProject
    with TheMethod
    with DefaultDomainValueBinding
    with ThrowAllPotentialExceptionsConfiguration
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l0.TypeLevelFieldAccessInstructions
    with l0.TypeLevelInvokeInstructions
    with l1.DefaultReferenceValuesBinding
    with l0.DefaultTypeLevelIntegerValues
    with l0.DefaultTypeLevelLongValues
    with l0.TypeLevelPrimitiveValuesConversions
    with l0.TypeLevelLongValuesShiftOperators
