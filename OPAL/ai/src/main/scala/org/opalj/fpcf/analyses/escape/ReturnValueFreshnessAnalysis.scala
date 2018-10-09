/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import scala.annotation.switch
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.ExtensibleGetter
import org.opalj.fpcf.properties.ExtensibleLocalField
import org.opalj.fpcf.properties.ExtensibleLocalFieldWithGetter
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.Getter
import org.opalj.fpcf.properties.LocalField
import org.opalj.fpcf.properties.LocalFieldWithGetter
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.NoLocalField
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VExtensibleGetter
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VGetter
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.ai.common.DefinitionSite
import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.tac.Assignment
import org.opalj.tac.GetField
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.fpcf.properties.TACAI

class ReturnValueFreshnessState(val dm: DefinedMethod) {
    private[this] var returnValueDependees: Set[EOptionP[DeclaredMethod, Property]] = Set.empty
    private[this] var fieldDependees: Set[EOptionP[Field, FieldLocality]] = Set.empty
    private[this] var defSiteDependees: Set[EOptionP[DefinitionSite, EscapeProperty]] = Set.empty
    private[this] var tacaiDependee: Option[EOptionP[Method, TACAI]] = None

    private[this] var upperBound: ReturnValueFreshness = FreshReturnValue

    def dependees: Set[EOptionP[Entity, Property]] = {
        returnValueDependees ++ fieldDependees ++ defSiteDependees ++ tacaiDependee
    }

    def hasDependees: Boolean = dependees.nonEmpty

    def hasTacaiDependee: Boolean = tacaiDependee.isDefined

    def addMethodDependee(epOrEpk: EOptionP[DeclaredMethod, Property]): Unit = {
        returnValueDependees += epOrEpk
    }

    def addFieldDependee(epOrEpk: EOptionP[Field, FieldLocality]): Unit = {
        fieldDependees += epOrEpk
    }

    def addDefSiteDependee(epOrEpk: EOptionP[DefinitionSite, EscapeProperty]): Unit = {
        defSiteDependees += epOrEpk
    }

    def removeMethodDependee(epOrEpk: EOptionP[DeclaredMethod, Property]): Unit = {
        returnValueDependees = returnValueDependees.filter(other ⇒ (other.e ne epOrEpk.e) || other.pk != epOrEpk.pk)
    }

    def removeFieldDependee(epOrEpk: EOptionP[Field, FieldLocality]): Unit = {
        fieldDependees = fieldDependees.filter(other ⇒ (other.e ne epOrEpk.e) || other.pk != epOrEpk.pk)
    }

    def removeDefSiteDependee(epOrEpk: EOptionP[DefinitionSite, EscapeProperty]): Unit = {
        defSiteDependees = defSiteDependees.filter(other ⇒ (other.e ne epOrEpk.e) || other.pk != epOrEpk.pk)
    }

    def updateTacaiDependee(epOrEpk: EOptionP[Method, TACAI]): Unit = {
        if (epOrEpk.isFinal) tacaiDependee = None
        else tacaiDependee = Some(epOrEpk)
    }

    def atMost(property: ReturnValueFreshness): Unit = {
        upperBound = upperBound meet property
    }

    def ubRVF: ReturnValueFreshness = upperBound
}

/**
 * This analysis determines for a given method whether the return value is a fresh object
 * that is created by the method (or its callees) and that does not escape other than
 * [[org.opalj.fpcf.properties.EscapeViaReturn]].
 *
 * In other words, it aggregates the escape information for allocation-sites that are used as return
 * value.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class ReturnValueFreshnessAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    private[this] val declaredMethods = project.get(DeclaredMethodsKey)
    private[this] val definitionSites = project.get(DefinitionSitesKey)
    private[this] val isOverridableMethod = project.get(IsOverridableMethodKey)

    /**
     * Ensures that we invoke [[doDetermineFreshness]] for [[org.opalj.br.DefinedMethod]]s only.
     */
    def determineFreshness(e: Entity): PropertyComputationResult = e match {
        case dm: DefinedMethod if dm.definedMethod.classFile.thisType == dm.declaringClassType ⇒ doDetermineFreshness(dm)

        // if the method is inherited, query the result for the one in its defining class
        case dm: DefinedMethod ⇒

            def handleReturnValueFreshness(
                eOptP: SomeEOptionP
            ): PropertyComputationResult = eOptP match {
                case FinalEP(_, p) ⇒ Result(e, p)
                case IntermediateEP(_, lb, ub) ⇒
                    IntermediateResult(
                        e, lb, ub,
                        Set(eOptP), handleReturnValueFreshness, CheapPropertyComputation
                    )
                case _ ⇒
                    IntermediateResult(
                        e, NoFreshReturnValue, FreshReturnValue,
                        Set(eOptP), handleReturnValueFreshness, CheapPropertyComputation
                    )
            }

            handleReturnValueFreshness(
                propertyStore(declaredMethods(dm.definedMethod), ReturnValueFreshness.key)
            )

        case _ ⇒ throw new RuntimeException(s"Unsupported entity $e")
    }

    /**
     * Determines the return value freshness for an [[org.opalj.br.DefinedMethod]].
     */
    def doDetermineFreshness(dm: DefinedMethod): PropertyComputationResult = {
        if (dm.descriptor.returnType.isBaseType || dm.descriptor.returnType.isVoidType)
            return Result(dm, PrimitiveReturnValue);

        if (dm.declaringClassType.isArrayType) {
            if (dm.name == "clone" && dm.descriptor == MethodDescriptor.JustReturnsObject) {
                return Result(dm, FreshReturnValue); // array.clone returns fresh value
            }
        }

        val m = dm.definedMethod
        if (m.body.isEmpty) // Can't analyze a method without body
            return Result(dm, NoFreshReturnValue);

        implicit val state: ReturnValueFreshnessState = new ReturnValueFreshnessState(dm)

        val codeO = getTACAICode(m)

        if (codeO.isEmpty)
            return IntermediateResult(
                dm,
                NoFreshReturnValue,
                FreshReturnValue,
                state.dependees,
                continuation
            );

        determineFreshnessForMethod(dm, codeO.get)
    }

    def determineFreshnessForMethod(
        dm:   DefinedMethod,
        code: Array[Stmt[V]]
    )(implicit state: ReturnValueFreshnessState): PropertyComputationResult = {
        val m = dm.definedMethod
        implicit val p: SomeProject = project

        // for every return-value statement check the def-sites
        for {
            ReturnValue(_, expr) ← code
            defSite ← expr.asVar.definedBy
        } {

            // parameters are not fresh by definition
            if (defSite < 0)
                return Result(dm, NoFreshReturnValue);

            val Assignment(pc, _, rhs) = code(defSite)

            // const values are handled as fresh
            if (!rhs.isConst) {

                // check if the variable is escaped
                val escape = propertyStore(definitionSites(m, pc), EscapeProperty.key)
                if (handleEscapeProperty(escape))
                    return Result(dm, NoFreshReturnValue);

                val isNotFresh = (rhs.astID: @switch) match {

                    case New.ASTID | NewArray.ASTID ⇒ false // fresh by definition

                    // Values from local fields are fresh if the object is fresh =>
                    // report these as [[org.opalj.fpcf.properties.Getter]]
                    case GetField.ASTID ⇒
                        val GetField(_, dc, name, fieldType, objRef) = rhs

                        // Only a getter if the field is accessed on the method's receiver object
                        if (objRef.asVar.definedBy != IntTrieSet(tac.OriginOfThis))
                            return Result(dm, NoFreshReturnValue);

                        val field = project.resolveFieldReference(dc, name, fieldType) match {
                            case Some(f) ⇒ f
                            case _       ⇒ return Result(dm, NoFreshReturnValue);
                        }

                        val locality = propertyStore(field, FieldLocality.key)
                        handleFieldLocalityProperty(locality)

                    case StaticFunctionCall.ASTID ⇒
                        val callee = rhs.asStaticFunctionCall.resolveCallTarget
                        handleConcreteCall(callee)

                    case NonVirtualFunctionCall.ASTID ⇒
                        val callee = rhs.asNonVirtualFunctionCall.resolveCallTarget(
                            dm.declaringClassType.asObjectType
                        )
                        handleConcreteCall(callee)

                    case VirtualFunctionCall.ASTID ⇒
                        handleVirtualCall(rhs.asVirtualFunctionCall)

                    // other kinds of assignments like GetStatic etc.
                    case _ ⇒ return Result(dm, NoFreshReturnValue);

                }

                if (isNotFresh)
                    return Result(dm, NoFreshReturnValue);
            }
        }

        returnResult
    }

    /**
     * Returns the TACode for a method if available, registering dependencies as necessary.
     */
    def getTACAICode(
        method: Method
    )(implicit state: ReturnValueFreshnessState): Option[Array[Stmt[V]]] = {
        val tacai = propertyStore(method, TACAI.key)

        state.updateTacaiDependee(tacai)

        if (tacai.hasProperty) tacai.ub.tac.map(_.stmts)
        else None
    }

    /**
     * Handles the effect of a virtual call on the return value freshness.
     * @return false if the return value may still be fresh, true otherwise.
     * @note Adds dependees as necessary.
     */
    def handleVirtualCall(
        callSite: VirtualFunctionCall[V]
    )(
        implicit
        state: ReturnValueFreshnessState
    ): Boolean = {
        val VirtualFunctionCall(_, dc, _, name, desc, receiver, _) = callSite

        val value = receiver.asVar.value.asReferenceValue
        val dm = state.dm
        val m = dm.definedMethod
        val thisType = m.classFile.thisType

        val receiverType = value.valueType

        if (receiverType.isEmpty) {
            false // Receiver is null, call will never be executed
        } else if (receiverType.get.isArrayType) {
            val callee = project.instanceCall(ObjectType.Object, ObjectType.Object, name, desc)
            handleConcreteCall(callee)
        } else if (value.isPrecise) {
            val callee = project.instanceCall(thisType, receiverType.get, name, desc)
            handleConcreteCall(callee)
        } else {
            val callee = declaredMethods(
                dc.asObjectType,
                thisType.packageName,
                receiverType.get.asObjectType,
                name,
                desc
            )

            // unknown method
            if (!callee.hasSingleDefinedMethod ||
                isOverridableMethod(callee.definedMethod).isYesOrUnknown)
                return true;

            val rvf = propertyStore(callee, VirtualMethodReturnValueFreshness.key)
            handleReturnValueFreshness(rvf)
        }
    }

    /**
     * Handles the influence of a monomorphic call on the return value freshness.
     * @return false if the return value may still be fresh, true otherwise.
     * @note Adds dependees as necessary.
     */
    def handleConcreteCall(
        callee: org.opalj.Result[Method]
    )(
        implicit
        state: ReturnValueFreshnessState
    ): Boolean = {
        if (callee.isEmpty) // Unknown method, not found in the scope of the current project
            return true;

        val dmCallee = declaredMethods(callee.value)

        if (dmCallee eq state.dm) {
            false // Recursive calls don't influence return value freshness
        } else {
            handleReturnValueFreshness(propertyStore(dmCallee, ReturnValueFreshness.key))
        }
    }

    /**
     * Handles the influence of an escape property on the return value freshness.
     * @return false if the return value may still be fresh, true otherwise.
     * @note (Re-)Adds dependees as necessary.
     */
    def handleEscapeProperty(
        ep: EOptionP[DefinitionSite, EscapeProperty]
    )(
        implicit
        state: ReturnValueFreshnessState
    ): Boolean = ep match {
        case FinalEP(_, NoEscape | EscapeInCallee) ⇒
            //throw new RuntimeException(s"unexpected result $ep for entity ${state.dm}")
            false // TODO this has happened - why?

        case FinalEP(_, EscapeViaReturn) ⇒ false

        case FinalEP(_, AtMost(_))       ⇒ true

        case FinalEP(_, _)               ⇒ true // Escape state is worse than via return

        case IntermediateEP(_, _, NoEscape | EscapeInCallee) ⇒
            state.addDefSiteDependee(ep)
            false

        case IntermediateEP(_, _, EscapeViaReturn) ⇒
            state.addDefSiteDependee(ep)
            false

        case IntermediateEP(_, _, AtMost(_)) ⇒ true

        case IntermediateEP(_, _, _)         ⇒ true // Escape state is worse than via return

        case _ ⇒
            state.addDefSiteDependee(ep)
            false
    }

    /**
     * Handles the influence of a field locality property on the return value freshness.
     * @return false if the return value may still be fresh, true otherwise.
     * @note (Re-)Adds dependees as necessary.
     */
    def handleFieldLocalityProperty(
        ep: EOptionP[Field, FieldLocality]
    )(
        implicit
        state: ReturnValueFreshnessState
    ): Boolean = ep match {
        case FinalEP(_, LocalFieldWithGetter) ⇒
            state.atMost(Getter)
            false

        case IntermediateEP(_, _, LocalFieldWithGetter) ⇒
            state.atMost(Getter)
            state.addFieldDependee(ep)
            false

        case FinalEP(_, NoLocalField) ⇒
            true

        case FinalEP(_, ExtensibleLocalFieldWithGetter) ⇒
            state.atMost(ExtensibleGetter)
            false

        case IntermediateEP(_, _, ExtensibleLocalFieldWithGetter) ⇒
            state.atMost(ExtensibleGetter)
            state.addFieldDependee(ep)
            false

        case FinalEP(_, LocalField | ExtensibleLocalField) ⇒
            // The value is returned, the field can not be local!
            throw new RuntimeException(s"unexpected result $ep for entity ${state.dm}")

        case _ ⇒
            state.addFieldDependee(ep)
            false
    }

    /**
     * Handles the influence of a callee's return value freshness on the return value freshness.
     * @return false if the return value may still be fresh, true otherwise.
     * @note (Re-)Adds dependees as necessary.
     */
    def handleReturnValueFreshness(
        ep: EOptionP[DeclaredMethod, Property]
    )(implicit state: ReturnValueFreshnessState): Boolean = ep match {
        case FinalEP(_, NoFreshReturnValue | VNoFreshReturnValue) ⇒ true

        case FinalEP(_, FreshReturnValue | VFreshReturnValue)     ⇒ false

        //IMPROVE: We can still be a getter if the callee has the same receiver
        case EPS(_, _, Getter | VGetter)                          ⇒ true

        case EPS(_, _, ExtensibleGetter | VExtensibleGetter)      ⇒ true

        case IntermediateEP(_, _, FreshReturnValue | VFreshReturnValue) ⇒
            state.addMethodDependee(ep)
            false

        case _: EPS[_, _] ⇒
            //TODO This currently happens because of a JSR/RET problem with the TAC
            // - restore the exception once this is fixed!
            //throw new RuntimeException(s"unexpected property $ep for entity ${state.dm}")
            false

        case _ ⇒
            state.addMethodDependee(ep)
            false
    }

    /**
     * A continuation function, that handles updates for the escape state.
     */
    def continuation(
        someEPS: SomeEPS
    )(implicit state: ReturnValueFreshnessState): PropertyComputationResult = {
        val dm = state.dm

        someEPS.e match {
            case _: DefinitionSite ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]
                state.removeDefSiteDependee(newEP)
                if (handleEscapeProperty(newEP))
                    return Result(dm, NoFreshReturnValue);

            case _: DeclaredMethod ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Property]]
                state.removeMethodDependee(newEP)
                if (handleReturnValueFreshness(newEP))
                    return Result(dm, NoFreshReturnValue);

            case _: Field ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[Field, FieldLocality]]
                state.removeFieldDependee(newEP)
                if (handleFieldLocalityProperty(newEP))
                    return Result(dm, NoFreshReturnValue);

            case _: Method ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[Method, TACAI]]
                state.updateTacaiDependee(newEP)
                return determineFreshnessForMethod(dm, newEP.ub.tac.get.stmts);
        }

        returnResult
    }

    def returnResult(implicit state: ReturnValueFreshnessState): PropertyComputationResult = {
        if (state.hasDependees)
            IntermediateResult(
                state.dm,
                NoFreshReturnValue,
                state.ubRVF,
                state.dependees,
                continuation,
                if (state.hasTacaiDependee) DefaultPropertyComputation else CheapPropertyComputation
            )
        else
            Result(state.dm, state.ubRVF)
    }
}

sealed trait ReturnValueFreshnessAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(ReturnValueFreshness)

    final override def uses: Set[PropertyKind] = {
        Set(TACAI, EscapeProperty, VirtualMethodReturnValueFreshness, FieldLocality)
    }

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerReturnValueFreshnessAnalysis
    extends ReturnValueFreshnessAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val declaredMethods =
            p.get(DeclaredMethodsKey).declaredMethods.filter(_.hasSingleDefinedMethod)
        val analysis = new ReturnValueFreshnessAnalysis(p)
        ps.scheduleEagerComputationsForEntities(declaredMethods)(analysis.determineFreshness)
        analysis
    }
}

object LazyReturnValueFreshnessAnalysis
    extends ReturnValueFreshnessAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new ReturnValueFreshnessAnalysis(p)
        ps.registerLazyPropertyComputation(ReturnValueFreshness.key, analysis.determineFreshness)
        analysis
    }
}
