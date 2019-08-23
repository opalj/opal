/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import scala.annotation.switch

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeInterimEP
import org.opalj.fpcf.UBP
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.ExtensibleLocalField
import org.opalj.br.fpcf.properties.FreshReturnValue
import org.opalj.br.fpcf.properties.Getter
import org.opalj.br.fpcf.properties.LocalField
import org.opalj.br.fpcf.properties.LocalFieldWithGetter
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.properties.NoFreshReturnValue
import org.opalj.br.fpcf.properties.PrimitiveReturnValue
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.FieldLocality
import org.opalj.br.fpcf.properties.ReturnValueFreshness
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.ExtensibleGetter
import org.opalj.br.fpcf.properties.ExtensibleLocalFieldWithGetter
import org.opalj.br.fpcf.properties.NoLocalField
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI

class ReturnValueFreshnessState(val dm: DefinedMethod) {
    private[this] var returnValueDependees: Map[DeclaredMethod, EOptionP[DeclaredMethod, ReturnValueFreshness]] = Map.empty
    private[this] var fieldDependees: Map[Field, EOptionP[Field, FieldLocality]] = Map.empty
    private[this] var defSiteDependees: Map[DefinitionSite, EOptionP[DefinitionSite, EscapeProperty]] = Map.empty
    private[this] var tacaiDependee: Option[EOptionP[Method, TACAI]] = None
    private[this] var _calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = None

    private[this] var _callSitePCs: IntTrieSet = IntTrieSet.empty

    private[this] var upperBound: ReturnValueFreshness = FreshReturnValue

    // TODO: Use EOptionPSet
    def dependees: Traversable[EOptionP[Entity, Property]] = {
        (returnValueDependees.valuesIterator ++
            fieldDependees.valuesIterator ++
            defSiteDependees.valuesIterator ++
            tacaiDependee.iterator ++
            _calleesDependee.iterator.filter(_.isRefinable)).toTraversable
    }

    def hasDependees: Boolean = {
        returnValueDependees.nonEmpty ||
            fieldDependees.nonEmpty ||
            defSiteDependees.nonEmpty ||
            tacaiDependee.nonEmpty ||
            _calleesDependee.exists(_.isRefinable)
    }

    def hasTacaiDependee: Boolean = tacaiDependee.isDefined

    def addMethodDependee(epOrEpk: EOptionP[DeclaredMethod, ReturnValueFreshness]): Unit = {
        assert(!returnValueDependees.contains(epOrEpk.e))
        returnValueDependees += epOrEpk.e → epOrEpk
    }

    def addFieldDependee(epOrEpk: EOptionP[Field, FieldLocality]): Unit = {
        assert(!fieldDependees.contains(epOrEpk.e))
        fieldDependees += epOrEpk.e → epOrEpk
    }

    def addDefSiteDependee(epOrEpk: EOptionP[DefinitionSite, EscapeProperty]): Unit = {
        assert(!defSiteDependees.contains(epOrEpk.e))
        defSiteDependees += epOrEpk.e → epOrEpk
    }

    def containsMethodDependee(epOrEpk: EOptionP[DeclaredMethod, ReturnValueFreshness]): Boolean = {
        returnValueDependees.contains(epOrEpk.e)
    }

    def containsFieldDependee(epOrEpk: EOptionP[Field, FieldLocality]): Boolean = {
        fieldDependees.contains(epOrEpk.e)
    }

    def containsDefSiteDependee(epOrEpk: EOptionP[DefinitionSite, EscapeProperty]): Boolean = {
        defSiteDependees.contains(epOrEpk.e)
    }

    def setCalleesDependee(epOrEpk: EOptionP[DeclaredMethod, Callees]): Unit = {
        _calleesDependee = Some(epOrEpk)
    }

    def calleesDependee: Option[EOptionP[DeclaredMethod, Callees]] = _calleesDependee

    def addCallSitePC(pc: Int): Unit = { _callSitePCs += pc }

    def callSitePCs: IntTrieSet = _callSitePCs

    def removeMethodDependee(epOrEpk: EOptionP[DeclaredMethod, Property]): Unit = {
        returnValueDependees -= epOrEpk.e
    }

    def removeFieldDependee(epOrEpk: EOptionP[Field, FieldLocality]): Unit = {
        fieldDependees -= epOrEpk.e
    }

    def removeDefSiteDependee(epOrEpk: EOptionP[DefinitionSite, EscapeProperty]): Unit = {
        defSiteDependees -= epOrEpk.e
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
 * [[org.opalj.br.fpcf.properties.EscapeViaReturn]].
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

    private[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val definitionSites = project.get(DefinitionSitesKey)

    /**
     * Ensures that we invoke [[doDetermineFreshness]] for [[org.opalj.br.DefinedMethod]]s only.
     */
    def determineFreshness(e: Entity): ProperPropertyComputationResult = e match {
        case dm: DefinedMethod if dm.definedMethod.classFile.thisType == dm.declaringClassType ⇒ doDetermineFreshness(dm)

        // if the method is inherited, query the result for the one in its defining class
        case dm: DefinedMethod ⇒

            def handleReturnValueFreshness(
                eOptP: SomeEOptionP
            ): ProperPropertyComputationResult = eOptP match {
                case FinalP(p) ⇒ Result(e, p)
                case InterimLUBP(lb, ub) ⇒
                    InterimResult.create(
                        e,
                        lb,
                        ub,
                        Set(eOptP),
                        handleReturnValueFreshness
                    )
                case _ ⇒
                    InterimResult(
                        e,
                        NoFreshReturnValue,
                        FreshReturnValue,
                        Set(eOptP),
                        handleReturnValueFreshness
                    )
            }

            handleReturnValueFreshness(
                propertyStore(declaredMethods(dm.definedMethod), ReturnValueFreshness.key)
            )

        // We treat VirtualDeclaredMethods and MultipleDefinedMethods as NoFreshReturnValue for now
        case dm: DeclaredMethod ⇒ Result(dm, NoFreshReturnValue)

        case _                  ⇒ throw new RuntimeException(s"Unsupported entity $e")
    }

    /**
     * Determines the return value freshness for an [[org.opalj.br.DefinedMethod]].
     */
    def doDetermineFreshness(dm: DefinedMethod): ProperPropertyComputationResult = {
        if (dm.descriptor.returnType.isBaseType || dm.descriptor.returnType.isVoidType)
            return Result(dm, PrimitiveReturnValue);

        val m = dm.definedMethod
        if (m.body.isEmpty) // Can't analyze a method without body
            return Result(dm, NoFreshReturnValue);

        implicit val state: ReturnValueFreshnessState = new ReturnValueFreshnessState(dm)

        val codeO = getTACAICode(m)

        if (codeO.isEmpty)
            return InterimResult(
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
    )(implicit state: ReturnValueFreshnessState): ProperPropertyComputationResult = {
        val m = dm.definedMethod

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
                if (!state.containsDefSiteDependee(escape) && handleEscapeProperty(escape))
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
                        if (!state.containsFieldDependee(locality))
                            handleFieldLocalityProperty(locality)
                        else
                            false // we already handled that entity earlier

                    case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                        VirtualFunctionCall.ASTID ⇒
                        handleCallSite(dm, pc)

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

        if (tacai.hasUBP && tacai.ub.tac.isDefined) tacai.ub.tac.map(_.stmts)
        else None
    }

    def handleCallSite(caller: DeclaredMethod, pc: Int)(
        implicit
        state: ReturnValueFreshnessState
    ): Boolean = {
        if (state.calleesDependee.isEmpty)
            state.setCalleesDependee(propertyStore(caller, Callees.key))
        val calleesEP = state.calleesDependee.get

        if (calleesEP.isEPK) {
            state.addCallSitePC(pc)
            false
        } else {
            calleesEP.ub.callees(pc).exists { callee ⇒
                (callee ne caller) && // Recursive calls don't influence return value freshness
                    {
                        val rvf = propertyStore(callee, ReturnValueFreshness.key)
                        !state.containsMethodDependee(rvf) && handleReturnValueFreshness(rvf)
                    }
            }
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
        case FinalP(NoEscape | EscapeInCallee) ⇒
            //throw new RuntimeException(s"unexpected result $ep for entity ${state.dm}")
            false // TODO this has happened - why?

        case FinalP(EscapeViaReturn)                    ⇒ false

        case FinalP(AtMost(_))                          ⇒ true

        case _: FinalEP[DefinitionSite, EscapeProperty] ⇒ true // Escape state is worse than via return

        case InterimUBP(NoEscape | EscapeInCallee) ⇒
            state.addDefSiteDependee(ep)
            false

        case InterimUBP(EscapeViaReturn) ⇒
            state.addDefSiteDependee(ep)
            false

        case InterimUBP(AtMost(_)) ⇒ true

        case _: SomeInterimEP      ⇒ true // Escape state is worse than via return

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
        case FinalP(LocalFieldWithGetter) ⇒
            state.atMost(Getter)
            false

        case InterimUBP(LocalFieldWithGetter) ⇒
            state.atMost(Getter)
            state.addFieldDependee(ep)
            false

        case FinalP(NoLocalField) ⇒
            true

        case FinalP(ExtensibleLocalFieldWithGetter) ⇒
            state.atMost(ExtensibleGetter)
            false

        case InterimUBP(ExtensibleLocalFieldWithGetter) ⇒
            state.atMost(ExtensibleGetter)
            state.addFieldDependee(ep)
            false

        case FinalP(LocalField | ExtensibleLocalField) ⇒
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
        ep: EOptionP[DeclaredMethod, ReturnValueFreshness]
    )(implicit state: ReturnValueFreshnessState): Boolean = ep match {
        case FinalP(NoFreshReturnValue) ⇒ true

        case FinalP(FreshReturnValue)   ⇒ false

        case UBP(PrimitiveReturnValue)  ⇒ false

        //IMPROVE: We can still be a getter if the callee has the same receiver
        case UBP(Getter)                ⇒ true

        case UBP(ExtensibleGetter)      ⇒ true

        case InterimUBP(FreshReturnValue) ⇒
            state.addMethodDependee(ep)
            false

        case _: SomeEPS ⇒
            throw new RuntimeException(s"unexpected property $ep for entity ${state.dm}")

        case _ ⇒
            state.addMethodDependee(ep)
            false
    }

    /**
     * A continuation function, that handles updates for the escape state.
     */
    def continuation(
        someEPS: SomeEPS
    )(implicit state: ReturnValueFreshnessState): ProperPropertyComputationResult = {
        val dm = state.dm

        someEPS.pk match {
            case EscapeProperty.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]
                state.removeDefSiteDependee(newEP)
                if (handleEscapeProperty(newEP))
                    return Result(dm, NoFreshReturnValue);

            case ReturnValueFreshness.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, ReturnValueFreshness]]
                state.removeMethodDependee(newEP)
                if (handleReturnValueFreshness(newEP)) {
                    return Result(dm, NoFreshReturnValue);
                }

            case FieldLocality.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[Field, FieldLocality]]
                state.removeFieldDependee(newEP)
                if (handleFieldLocalityProperty(newEP))
                    return Result(dm, NoFreshReturnValue);

            case TACAI.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[Method, TACAI]]
                state.updateTacaiDependee(newEP)
                if (newEP.ub.tac.isDefined)
                    return determineFreshnessForMethod(dm, newEP.ub.tac.get.stmts);

            case Callees.key ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Callees]]
                state.setCalleesDependee(newEP)
                if (state.callSitePCs.exists(pc ⇒ handleCallSite(dm, pc))) {
                    return Result(dm, NoFreshReturnValue);
                }
        }

        returnResult
    }

    def returnResult(implicit state: ReturnValueFreshnessState): ProperPropertyComputationResult = {
        if (state.hasDependees)
            InterimResult(
                state.dm,
                NoFreshReturnValue,
                state.ubRVF,
                state.dependees,
                continuation
            )
        else
            Result(state.dm, state.ubRVF)
    }
}

sealed trait ReturnValueFreshnessAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(ReturnValueFreshness)

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, DefinitionSitesKey)

    override def uses: Set[PropertyBounds] = {
        Set(
            PropertyBounds.ub(TACAI),
            PropertyBounds.ub(EscapeProperty),
            PropertyBounds.ub(Callees),
            PropertyBounds.ub(FieldLocality),
            PropertyBounds.ub(ReturnValueFreshness)
        )
    }
}

object EagerReturnValueFreshnessAnalysis
    extends ReturnValueFreshnessAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val declaredMethods = p.get(DeclaredMethodsKey)

        val methods = declaredMethods.declaredMethods
        val callersProperties = ps(methods.toTraversable, Callers)
        assert(callersProperties.forall(_.isFinal))

        val reachableMethods = callersProperties.filterNot(_.asFinal.p == NoCallers).map(_.e).toSet

        val analysis = new ReturnValueFreshnessAnalysis(p)
        ps.scheduleEagerComputationsForEntities(reachableMethods)(analysis.determineFreshness)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def uses: Set[PropertyBounds] = super.uses + PropertyBounds.finalP(Callers)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

object LazyReturnValueFreshnessAnalysis
    extends ReturnValueFreshnessAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new ReturnValueFreshnessAnalysis(p)
        ps.registerLazyPropertyComputation(ReturnValueFreshness.key, analysis.determineFreshness)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}
