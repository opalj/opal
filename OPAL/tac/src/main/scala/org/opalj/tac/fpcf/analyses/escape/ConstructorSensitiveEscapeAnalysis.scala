/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeInterimEP
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaHeapObject
import org.opalj.br.fpcf.properties.EscapeViaParameter
import org.opalj.br.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaStaticField
import org.opalj.br.fpcf.properties.GlobalEscape
import org.opalj.br.fpcf.properties.NoEscape

/**
 * Special handling for constructor calls, as the receiver of an constructor is always an
 * allocation site.
 * The constructor of Object does not escape the self reference by definition. For other
 * constructors, the inter-procedural chain will be processed until it reaches the Object
 * constructor or escapes. Is this the case, leastRestrictiveProperty will be set to the lower bound
 * of the current value and the calculated escape state.
 *
 * For non constructor calls, [[org.opalj.br.fpcf.properties.AtMost]]
 * ([[org.opalj.br.fpcf.properties.EscapeInCallee]]) of `e will be ` returned whenever the receiver or
 * a parameter is a use of defSite.
 *
 * @author Florian Kuebler
 */
trait ConstructorSensitiveEscapeAnalysis extends AbstractEscapeAnalysis {

    override type AnalysisContext <: AbstractEscapeAnalysisContext with PropertyStoreContainer with VirtualFormalParametersContainer with DeclaredMethodsContainer

    abstract protected[this] override def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        assert(call.name == "<init>", "method is not a constructor")
        assert(state.usesDefSite(call.receiver), "call receiver does not use def-site")

        // the object constructor will not escape the this local
        if (call.declaringClass eq ObjectType.Object)
            return ;

        // resolve the constructor
        val constructor = project.specialCall(
            context.targetMethodDeclaringClassType,
            call.declaringClass, call.isInterface, name = "<init>", call.descriptor
        )
        constructor match {
            case Success(callee) =>
                // check if the this local escapes in the callee

                val fp = context.virtualFormalParameters(context.declaredMethods(callee))(0)
                val fpEntity =
                    (
                        typeProvider.expandContext(
                            context.entity._1, declaredMethods(callee), call.pc
                        ),
                            fp
                    )
                if (fpEntity != context.entity) {
                    val escapeState = context.propertyStore(fpEntity, EscapeProperty.key)
                    if (!state.containsDependency(escapeState))
                        handleEscapeState(escapeState)
                }
            case /* unknown method */ _ => state.meetMostRestrictive(AtMost(NoEscape))
        }
    }

    private[this] def handleEscapeState(
        eOptionP: EOptionP[Entity, Property]
    )(
        implicit
        state: AnalysisState
    ): Unit = {
        eOptionP match {
            case FinalP(NoEscape) => //NOTHING TO DO

            case FinalP(GlobalEscape) =>
                state.meetMostRestrictive(GlobalEscape)

            case FinalP(EscapeViaStaticField) =>
                state.meetMostRestrictive(EscapeViaStaticField)

            case FinalP(EscapeViaHeapObject) =>
                state.meetMostRestrictive(EscapeViaHeapObject)

            case FinalP(EscapeInCallee) =>
                state.meetMostRestrictive(EscapeInCallee)

            case FinalP(AtMost(EscapeInCallee)) =>
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case FinalP(EscapeViaParameter) =>
                state.meetMostRestrictive(AtMost(NoEscape))

            case FinalP(EscapeViaAbnormalReturn) =>
                state.meetMostRestrictive(AtMost(NoEscape))

            case FinalP(EscapeViaParameterAndAbnormalReturn) =>
                state.meetMostRestrictive(AtMost(NoEscape))

            case FinalP(AtMost(NoEscape)) =>
                state.meetMostRestrictive(AtMost(NoEscape))

            case FinalP(AtMost(EscapeViaParameter)) =>
                state.meetMostRestrictive(AtMost(NoEscape))

            case FinalP(AtMost(EscapeViaAbnormalReturn)) =>
                state.meetMostRestrictive(AtMost(NoEscape))

            case FinalP(AtMost(EscapeViaParameterAndAbnormalReturn)) =>
                state.meetMostRestrictive(AtMost(NoEscape))

            case FinalP(p) =>
                throw new UnknownError(s"unexpected escape property ($p) for constructors")

            case ep @ InterimUBP(NoEscape) =>
                state.addDependency(ep)

            case ep @ InterimUBP(EscapeInCallee) =>
                state.meetMostRestrictive(EscapeInCallee)
                state.addDependency(ep)

            case ep @ InterimUBP(AtMost(EscapeInCallee)) =>
                state.meetMostRestrictive(AtMost(EscapeInCallee))
                state.addDependency(ep)

            case ep: SomeInterimEP =>
                state.meetMostRestrictive(AtMost(NoEscape))
                state.addDependency(ep)

            // result not yet finished
            case epk =>
                state.addDependency(epk)
        }
    }

    abstract override protected[this] def c(
        someEPS: SomeEPS
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {

        someEPS.e match {
            case (_: Context, VirtualFormalParameter(dm: DefinedMethod, -1)) if dm.definedMethod.isConstructor =>
                state.removeDependency(someEPS)
                handleEscapeState(someEPS)
                returnResult

            case _ => super.c(someEPS)
        }
    }
}
