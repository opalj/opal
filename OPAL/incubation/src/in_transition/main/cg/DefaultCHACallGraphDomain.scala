/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.MethodSignature
import org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration
import org.opalj.ai.domain.DefaultHandlingOfMethodResults
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.DefaultDomainValueBinding
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.IgnoreSynchronization
import org.opalj.ai.domain.l0
/**
 * Domain object which is used to calculate the call graph.
 *
 * @author Michael Eichberg
 */
class DefaultCHACallGraphDomain[Source](
        val project: Project[Source],
        val cache:   CallGraphCache[MethodSignature, Set[Method]],
        val method:  Method
) extends Domain
    with DefaultDomainValueBinding
    with ThrowAllPotentialExceptionsConfiguration
    with TheProject
    with TheMethod
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with l0.DefaultTypeLevelIntegerValues
    with l0.DefaultTypeLevelLongValues
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l0.TypeLevelPrimitiveValuesConversions
    with l0.TypeLevelLongValuesShiftOperators
    with l0.DefaultReferenceValuesBinding
    with l0.TypeLevelInvokeInstructions
    with l0.TypeLevelFieldAccessInstructions
