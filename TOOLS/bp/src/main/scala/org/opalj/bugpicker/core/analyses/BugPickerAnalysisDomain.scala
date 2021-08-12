/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.MethodSignature
import org.opalj.br.ReferenceType
import org.opalj.br.MethodDescriptor
import org.opalj.ai.CorrelationalDomain
import org.opalj.ai.domain
import org.opalj.ai.analyses.FieldValueInformation
import org.opalj.ai.analyses.MethodReturnValueInformation
import org.opalj.ai.domain.la.PerformInvocationsWithBasicVirtualMethodCallResolution
import org.opalj.ai.domain.l2.PerformInvocationsWithRecursionDetection
import org.opalj.ai.mapOperands
import org.opalj.ai.TheAI
import org.opalj.ai.domain.l2.CalledMethodsStore
import org.opalj.ai.TheMemoryLayout
import org.opalj.ai.util.XHTML
import org.opalj.ai.domain.l2.ChildPerformInvocationsWithRecursionDetection
import org.opalj.ai.AIResult
import org.opalj.ai.domain.l2.CalledMethodsStore
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.collection.immutable.Chain

/**
 * The base domain that is shared by all domains that are used to perform abstract
 * interpretations of methods.
 *
 * @author Michael Eichberg
 */
trait BaseBugPickerAnalysisDomain
    extends CorrelationalDomain
    with domain.TheProject
    with domain.TheMethod
    with domain.DefaultSpecialDomainValuesBinding
    with domain.ThrowAllPotentialExceptionsConfiguration
    //with domain.l0.TypeLevelFieldAccessInstructions
    with domain.la.RefinedTypeLevelFieldAccessInstructions
    with domain.l0.TypeLevelInvokeInstructions
    with domain.la.RefinedTypeLevelInvokeInstructions
    with domain.SpecialMethodsHandling
    with domain.l1.DefaultIntegerRangeValues
    // with domain.l1.DefaultIntegerSetValues
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with domain.l1.ConstraintsBetweenIntegerValues
    with domain.l1.DefaultLongSetValues
    with domain.l1.LongSetValuesShiftOperators
    with domain.l0.DefaultTypeLevelFloatValues
    with domain.l0.DefaultTypeLevelDoubleValues
    with domain.l1.ConcretePrimitiveValuesConversions
    //with domain.l1.DefaultReferenceValuesBinding [implicitly mixed in via StringValuesBinding]
    //with domain.l1.DefaultStringValuesBinding [implicitly mixed in via ClassValuesBinding]
    with domain.l1.NullPropertyRefinement
    with domain.l1.MaxArrayLengthRefinement
    with domain.DefaultHandlingOfMethodResults
    with domain.IgnoreSynchronization {
    // We want to get the special treatment of calls on "Class" objects
    // and do not want to perform invocations in this case;
    // hence, we have to mix in this domain AFTER the PerformInvocations domain!
    this: domain.l1.DefaultClassValuesBinding =>
}

/**
 * This is the fall back domain that is used to perform an abstract interpretation of
 * a method without invoking called methods.
 */
class FallbackBugPickerAnalysisDomain(
        val project:                                Project[URL],
        val fieldValueInformation:                  FieldValueInformation,
        val methodReturnValueInformation:           MethodReturnValueInformation,
        val cache:                                  CallGraphCache[MethodSignature, scala.collection.Set[Method]],
        override val maxCardinalityOfIntegerRanges: Long,
        override val maxCardinalityOfLongSets:      Int,
        val /*current*/ method:                     Method
) extends BaseBugPickerAnalysisDomain
    with domain.l1.DefaultClassValuesBinding
    with domain.l1.RecordAllThrownExceptions
    with domain.RecordCFG
    with domain.RecordDefUse

/**
 * The base domain that is shared by all domains that are used to perform abstract
 * interpretations of methods where methods are potentially called.
 */
trait BasePerformInvocationBugPickerAnalysisDomain
    extends BaseBugPickerAnalysisDomain
    with PerformInvocationsWithRecursionDetection
    with PerformInvocationsWithBasicVirtualMethodCallResolution
    with domain.l1.DefaultClassValuesBinding { callingDomain =>

    def debug: Boolean

    type CalledMethodDomain = InvocationBugPickerAnalysisDomain

    override protected[this] def doInvoke(
        method:             Method,
        calledMethodDomain: CalledMethodDomain
    )(
        parameters: calledMethodDomain.Locals
    ): AIResult { val domain: calledMethodDomain.type } = {
        val result = super.doInvoke(method, calledMethodDomain)(parameters)
        if (debug) {
            import result._
            org.opalj.io.writeAndOpen(
                org.opalj.ai.common.XHTML.dump(
                    Some(method.classFile),
                    Some(method),
                    method.body.get,
                    Some(
                        "Created: "+(new java.util.Date).toString+"<br>"+
                            "Domain: "+result.domain.getClass.getName+"<br>"+
                            XHTML.evaluatedInstructionsToXHTML(result.evaluated)
                    ),
                    result.domain
                )(cfJoins, result.operandsArray, result.localsArray),
                "AIResult",
                ".html"
            )
        }
        result
    }

    override def doInvoke(
        pc:       PC,
        method:   Method,
        operands: callingDomain.Operands,
        fallback: () => MethodCallResult
    ): MethodCallResult = {
        val result = super.doInvoke(pc, method, operands, fallback)
        if (debug) {
            println("the result of calling "+method.toJava+" is "+result)
        }
        result
    }

    val maxCallChainLength: Int

    def currentCallChainLength: Int

    def shouldInvocationBePerformed(calledMethod: Method): Boolean = {
        val result =
            maxCallChainLength > currentCallChainLength &&
                // TODO check me if the following makes sense:
                calledMethod.isPrivate && calledMethod.actualArgumentsCount != 1
        if (debug) {
            val i = if (result) " invokes " else " does not invoke "
            println(s"[$currentCallChainLength]"+
                method.toJava +
                i +
                calledMethod.toJava)
        }
        result
    }

    abstract override def invokevirtual(
        pc:             PC,
        declaringClass: ReferenceType,
        name:           String,
        descriptor:     MethodDescriptor,
        operands:       Operands
    ): MethodCallResult = {

        val result = super.invokevirtual(pc, declaringClass, name, descriptor, operands)
        if (debug) {
            println(s"[$currentCallChainLength] call result of "+
                declaringClass.toJava+" "+descriptor.toJava(name) + result)
        }
        result
    }
}

class InvocationBugPickerAnalysisDomain(
        val project:                                Project[URL],
        val fieldValueInformation:                  FieldValueInformation,
        val methodReturnValueInformation:           MethodReturnValueInformation,
        val cache:                                  CallGraphCache[MethodSignature, scala.collection.Set[Method]],
        override val maxCardinalityOfIntegerRanges: Long,
        override val maxCardinalityOfLongSets:      Int,
        val maxCallChainLength:                     Int,
        val callerDomain:                           BasePerformInvocationBugPickerAnalysisDomain,
        val /*current*/ method:                     Method,
        val currentCallChainLength:                 Int,
        val debug:                                  Boolean
) extends BasePerformInvocationBugPickerAnalysisDomain
    with domain.RecordMethodCallResults
    with domain.RecordLastReturnedValues
    with domain.RecordAllThrownExceptions
    with ChildPerformInvocationsWithRecursionDetection {
    callingDomain =>

    override def calledMethodDomain(method: Method) = {
        new InvocationBugPickerAnalysisDomain(
            project,
            fieldValueInformation,
            methodReturnValueInformation,
            cache,
            maxCardinalityOfIntegerRanges,
            maxCardinalityOfLongSets,
            maxCallChainLength,
            callingDomain,
            method, currentCallChainLength + 1,
            debug
        ) {
        }
    }

    def calledMethodAI = callerDomain.calledMethodAI

}

/**
 * The domain that is used to identify the issues.
 *
 * @author Michael Eichberg
 */
class RootBugPickerAnalysisDomain(
        val project:                                Project[URL],
        val fieldValueInformation:                  FieldValueInformation,
        val methodReturnValueInformation:           MethodReturnValueInformation,
        val cache:                                  CallGraphCache[MethodSignature, scala.collection.Set[Method]],
        override val maxCardinalityOfIntegerRanges: Long,
        override val maxCardinalityOfLongSets:      Int,
        val maxCallChainLength:                     Int,
        val /*current*/ method:                     Method,
        val debug:                                  Boolean,
        val frequentEvaluationWarningLevel:         Int                                                           = 256
) extends BasePerformInvocationBugPickerAnalysisDomain
    with TheAI[BaseBugPickerAnalysisDomain]
    with TheMemoryLayout // required to extract the initial operands
    // the following two are required to detect instructions that always throw
    // an exception (such as div by zero, a failing checkcast, a method call that
    // always fails etc.)
    with domain.l1.RecordAllThrownExceptions
    with domain.RecordCFG
    with domain.RecordDefUse { callingDomain =>

    final def currentCallChainLength: Int = 0
    final def calledMethodAI = ai
    final val coordinatingDomain = this

    // The called methods store is always only required at analysis time, at this point
    // in time the initial operands are available!
    lazy val calledMethodsStore: CalledMethodsStore { val domain: coordinatingDomain.type } = {
        val operands =
            localsArray(0).foldLeft(Chain.empty[DomainValue])((l, n) =>
                if (n ne null) n :&: l else l)
        CalledMethodsStore(coordinatingDomain, frequentEvaluationWarningLevel)(
            method, mapOperands(operands, coordinatingDomain)
        )
    }

    override def calledMethodDomain(method: Method) =
        new InvocationBugPickerAnalysisDomain(
            project,
            fieldValueInformation,
            methodReturnValueInformation,
            cache,
            maxCardinalityOfIntegerRanges,
            maxCardinalityOfLongSets,
            maxCallChainLength,
            callingDomain,
            method,
            currentCallChainLength + 1,
            debug
        )

}
