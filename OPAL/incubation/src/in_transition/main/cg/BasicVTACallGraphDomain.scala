/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import scala.collection.Set
import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.MethodSignature
import org.opalj.ai.domain._

/**
 * Domain object which is used to calculate the call graph using variable type analysis.
 *
 * @author Michael Eichberg
 */
class BasicVTACallGraphDomain[Source](
        val project: Project[Source],
        val cache:   CallGraphCache[MethodSignature, Set[Method]],
        val method:  Method
) extends CorrelationalDomain
    with DefaultDomainValueBinding
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
    with l0.DefaultReferenceValuesBinding
    with l0.TypeLevelInvokeInstructions
    with l0.TypeLevelFieldAccessInstructions
