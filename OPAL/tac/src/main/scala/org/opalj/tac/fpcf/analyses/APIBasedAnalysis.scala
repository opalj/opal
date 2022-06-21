/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.EPK
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.analyses.cg.ContextualAnalysis
import org.opalj.tac.fpcf.analyses.cg.TypeProvider

/**
 * A trait for analyses that model the result of the invocation of a specific
 * `apiMethod`.
 *
 * Each time a new caller of the [[apiMethod*]] is found in the
 * [[Callers]] property, [[handleNewCaller*]]
 * gets called.
 *
 * @note When `handleNewCaller` gets invoked, there is no guarantee that the caller's three-address
 *       code ([[org.opalj.tac.fpcf.properties.TACAI]]) is present in the property store, nor that
 *       it is final. If this is required, use the [[TACAIBasedAPIBasedAnalysis]]
 *       sub-trait.
 *
 * @author Florian Kuebler
 */
trait APIBasedAnalysis extends FPCFAnalysis with ContextualAnalysis {
    val apiMethod: DeclaredMethod

    implicit val typeProvider: TypeProvider
    implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)

    def handleNewCaller(
        calleeContext: ContextType, callerContext: ContextType, pc: Int, isDirect: Boolean
    ): ProperPropertyComputationResult

    final def registerAPIMethod(): ProperPropertyComputationResult = {
        val callersEOptP = ps(apiMethod, Callers.key)
        c(null)(callersEOptP)
    }

    private[this] def c(
        oldCallers: Callers
    )(callersEOptP: SomeEOptionP): ProperPropertyComputationResult =
        (callersEOptP: @unchecked) match {
            case EUBP(dm: DeclaredMethod, callersUB: Callers) =>
                var results: List[ProperPropertyComputationResult] = Nil
                if (callersUB.nonEmpty) {
                    callersUB.forNewCallerContexts(oldCallers, dm) {
                        (calleeContext, callerContext, pc, isDirect) =>
                            if (callerContext.hasContext) {
                                val caller = callerContext.method

                                // the call graph is only computed for virtual and single defined methods
                                assert(caller.isVirtualOrHasSingleDefinedMethod)

                                // we can not analyze virtual methods, as we do not have their bytecode
                                if (caller.hasSingleDefinedMethod) {
                                    results ::= handleNewCaller(
                                        calleeContext.asInstanceOf[ContextType],
                                        callerContext.asInstanceOf[ContextType],
                                        pc,
                                        isDirect
                                    )
                                }
                            }
                    }
                }

                if (callersEOptP.isRefinable)
                    results ::= InterimPartialResult(Set(callersEOptP), c(callersUB))

                Results(results)

            case _: EPK[_, _] => InterimPartialResult(Set(callersEOptP), c(null))
        }
}
