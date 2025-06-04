/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Base trait for analyses that are executed for every method that is reachable.
 * The analysis is performed by `processMethod`.
 *
 * Note that methods without a body are not processed unless `processMethodWithoutBody` is overridden
 *
 * @author Florian Kuebler
 */
trait ReachableMethodAnalysis extends FPCFAnalysis with TypeConsumerAnalysis {

    protected implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    final def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        val callersEOptP = propertyStore(declaredMethod, Callers.key)

        if (callersEOptP.isFinal && callersEOptP.ub == NoCallers) {
            return NoResult;
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
            processMethod(callersEOptP, NoCallers, tacEP.asEPS)
        } else {
            InterimPartialResult(Set(tacEP), continuationForTAC(declaredMethod))
        }
    }

    protected def processMethodWithoutBody(
        eOptP: EOptionP[DeclaredMethod, Callers]
    ): PropertyComputationResult = NoResult

    protected def processMethodWithoutBody(
        eOptP: EOptionP[DeclaredMethod, Callers],
        tacEP: EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        processMethod(eOptP, NoCallers, tacEP)
    }

    protected def processMethod(
        eOptP:      EOptionP[DeclaredMethod, Callers],
        oldCallers: Callers,
        tacEP:      EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val newCallers = if (eOptP.hasUBP) eOptP.ub else NoCallers
        var results: List[ProperPropertyComputationResult] = Nil

        newCallers.forNewCalleeContexts(oldCallers, eOptP.e) { calleeContext =>
            val theCalleeContext =
                if (calleeContext.hasContext) calleeContext.asInstanceOf[ContextType]
                else typeIterator.newContext(eOptP.e)
            results ::= processMethod(theCalleeContext, tacEP)
        }

        Results(
            InterimPartialResult(Set(eOptP), continuationForCallers(newCallers, tacEP)),
            results
        )
    }

    def processMethod(
        callContext: ContextType,
        tacEP:       EPS[Method, TACAI]
    ): ProperPropertyComputationResult

    protected def continuationForTAC(
        declaredMethod: DeclaredMethod
    )(someEPS: SomeEPS): ProperPropertyComputationResult = {
        someEPS match {
            case UBP(tac: TACAI) if tac.tac.isDefined =>
                processMethod(
                    propertyStore(declaredMethod, Callers.key),
                    NoCallers,
                    someEPS.asInstanceOf[EPS[Method, TACAI]]
                )
            case _ =>
                throw new IllegalArgumentException(s"unexpected eps $someEPS")
        }
    }

    private[this] def continuationForCallers(
        oldCallers: Callers,
        tacEP:      EPS[Method, TACAI]
    )(
        update: SomeEPS
    ): ProperPropertyComputationResult = {
        val newCallers = update.asInstanceOf[EPS[DeclaredMethod, Callers]]
        processMethod(newCallers, oldCallers, tacEP)
    }
}
