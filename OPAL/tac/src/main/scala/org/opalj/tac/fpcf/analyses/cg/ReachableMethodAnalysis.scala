/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.NoCallers
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Base trait for analyses that are executed for every method that is reachable.
 * The analysis is performed by `processMethod`.
 *
 * @author Florian Kuebler
 */
trait ReachableMethodAnalysis extends FPCFAnalysis with TypeConsumerAnalysis {

    protected implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    final def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        val callersEOptP = propertyStore(declaredMethod, Callers.key)
        (callersEOptP: @unchecked) match {
            case FinalP(NoCallers) =>
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] =>
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                }
            // the method is reachable, so we analyze it!
        }

        // we only allow defined methods
        if (!declaredMethod.hasSingleDefinedMethod)
            return processMethodWithoutBody(callersEOptP);

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return processMethodWithoutBody(callersEOptP);

        val tacEP = propertyStore(method, TACAI.key)

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined) {
            processMethod(callersEOptP, null, tacEP.asEPS)
        } else {
            InterimPartialResult(Set(tacEP), continuationForTAC(declaredMethod))
        }
    }

    protected val processesMethodsWithoutBody = false

    protected def processMethodWithoutBody(
        eOptP: EOptionP[DeclaredMethod, Callers]
    ): PropertyComputationResult = {
        if (processesMethodsWithoutBody) {
            processMethod(eOptP, null, null)
        } else
            NoResult
    }

    private[this] def processMethod(
        eOptP: EOptionP[DeclaredMethod, Callers], seen: Callers, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        var results: List[ProperPropertyComputationResult] = Nil
        eOptP.ub.forNewCalleeContexts(seen, eOptP.e) { calleeContext =>
            val theCalleeContext =
                if (calleeContext.hasContext) calleeContext.asInstanceOf[ContextType]
                else typeProvider.newContext(eOptP.e)
            results ::= processMethod(theCalleeContext, tacEP)
        }

        Results(
            InterimPartialResult(Set(eOptP), continuationForCallers(eOptP.ub, tacEP)),
            results
        )
    }

    def processMethod(
        callContext: ContextType, tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult

    protected def continuationForTAC(
        declaredMethod: DeclaredMethod
    )(someEPS: SomeEPS): ProperPropertyComputationResult = {
        someEPS match {
            case UBP(tac: TACAI) if tac.tac.isDefined =>
                processMethod(
                    propertyStore(declaredMethod, Callers.key),
                    null,
                    someEPS.asInstanceOf[EPS[Method, TACAI]]
                )
            case _ =>
                throw new IllegalArgumentException(s"unexpected eps $someEPS")
        }
    }

    private[this] def continuationForCallers(
        oldCallers: Callers, tacEP: EPS[Method, TACAI]
    )(
        update: SomeEPS
    ): ProperPropertyComputationResult = {
        val newCallers = update.asInstanceOf[EPS[DeclaredMethod, Callers]]
        processMethod(newCallers, oldCallers, tacEP)
    }

}
