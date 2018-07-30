/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import scala.collection.Set

import org.opalj.ai.CorrelationalDomain
import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.br.MethodSignature
import org.opalj.br.analyses.Project
import org.opalj.ai.domain.DefaultDomainValueBinding
import org.opalj.ai.domain.DefaultHandlingOfMethodResults
import org.opalj.ai.domain.IgnoreSynchronization
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.TheProject
import org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration
import org.opalj.ai.domain.SpecialMethodsHandling
import org.opalj.ai.domain.l0
import org.opalj.ai.domain.l1
import org.opalj.ai.domain.la
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.domain.DefaultRecordMethodCallResults
import org.opalj.ai.domain.la.PerformInvocationsWithBasicVirtualMethodCallResolution

/**
 * Domain object which can used to calculate a context-sensitive call graph.
 * This domain uses advanced domains for tracking primitive values to rule out
 * potential dead branches/method calls on dead branches.
 *
 * @author Michael Eichberg
 */
class CFACallGraphDomain[Source](
        val k:                            Int, // the maximum length of the call chain
        val project:                      Project[Source],
        val fieldValueInformation:        FieldValueInformation,
        val methodReturnValueInformation: MethodReturnValueInformation,
        val cache:                        CallGraphCache[MethodSignature, Set[Method]],
        val method:                       Method,
        val calledMethods:                Set[Method]                                  = Set.empty
) extends CorrelationalDomain
    with DefaultDomainValueBinding
    with ThrowAllPotentialExceptionsConfiguration
    with TheProject
    with TheMethod
    with DefaultHandlingOfMethodResults
    with IgnoreSynchronization
    with l1.DefaultReferenceValuesBinding
    with l1.NullPropertyRefinement
    with l1.DefaultIntegerRangeValues
    //with l0.DefaultTypeLevelIntegerValues
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with l1.ConstraintsBetweenIntegerValues
    with l0.DefaultTypeLevelLongValues
    //with l1.LongSetValuesShiftOperators
    with l0.TypeLevelPrimitiveValuesConversions
    with l0.TypeLevelLongValuesShiftOperators
    with l0.DefaultTypeLevelFloatValues
    with l0.DefaultTypeLevelDoubleValues
    with l1.MaxArrayLengthRefinement
    with l0.TypeLevelInvokeInstructions // the foundation
    with la.RefinedTypeLevelInvokeInstructions
    with SpecialMethodsHandling
    with la.RefinedTypeLevelFieldAccessInstructions
    with PerformInvocationsWithBasicVirtualMethodCallResolution
    with DefaultRecordMethodCallResults {
    callingDomain ⇒

    // we just want to be able to track "booleans"
    override def maxCardinalityOfIntegerRanges: Long = 2L

    def shouldInvocationBePerformed(method: Method): Boolean =
        (method ne this.method) &&
            // we only call methods where we have a chance that the return value
            // actually depends on the calling context and may directly affect
            // subsequent calls (i.e., if the return value is a primitive value
            // then we are not interested.) The return value of methods where the
            // return value does not depend on the calling context should
            // be precise based on the results of the pre-analyses.
            calledMethods.size < k && !calledMethods.contains(method) &&
            !method.returnType.isVoidType &&
            (
                !method.returnType.isObjectType ||
                classHierarchy.hasSubtypes(method.returnType.asObjectType).isYes
            )

    type CalledMethodDomain = CFACallGraphDomain[Source]

    override def calledMethodDomain(method: Method) =
        new CFACallGraphDomain(
            k,
            project,
            fieldValueInformation,
            methodReturnValueInformation,
            cache,
            method,
            calledMethods + callingDomain.method
        )

    def calledMethodAI: BaseAI.type = BaseAI

    override protected[this] def doInvoke(
        pc:       PC,
        method:   Method,
        operands: Operands,
        fallback: () ⇒ MethodCallResult
    ): MethodCallResult = {

        val result = super.doInvoke(pc, method, operands, fallback)
        //        if (calledMethods.isEmpty)
        //            println(s"[info - call graph] ${callingDomain.method.toJava(callingDomain.classFile)}:$pc the result of calling ${method.toJava(definingClass)} is $result")
        result
    }
}
