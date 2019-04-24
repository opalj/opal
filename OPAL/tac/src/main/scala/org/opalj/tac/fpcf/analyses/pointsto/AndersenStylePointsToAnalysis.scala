/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.fpcf.UBPS
import org.opalj.value.ValueInformation
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.pointsto.properties.PointsTo
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.ObjectType
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI

class PointsToState private (
        private[pointsto] val method:   DefinedMethod,
        private[this] var _tacDependee: Option[EOptionP[Method, TACAI]]
) {

    val pointsToSets: mutable.Map[Entity, UIDSet[ObjectType]] = mutable.Map.empty

    // todo document
    // if we get an update for e: we have to update all points-to sets for the entities in map(e)
    val _dependeeToDependers: mutable.Map[Entity, mutable.Set[Entity]] = mutable.Map.empty

    val _dependerToDependees: mutable.Map[Entity, mutable.Set[Entity]] = mutable.Map.empty

    val _dependees: mutable.Map[Entity, EOptionP[Entity, PointsTo]] = mutable.Map.empty

    def tac: TACode[TACMethodParameter, DUVar[ValueInformation]] = {
        assert(_tacDependee.isDefined)
        assert(_tacDependee.get.ub.tac.isDefined)
        _tacDependee.get.ub.tac.get
    }

    def dependees: Iterable[EOptionP[Entity, Property]] = {
        _tacDependee.filterNot(_.isFinal) ++ _dependees.values
    }

    def hasOpenDependees: Boolean = {
        !_tacDependee.forall(_.isFinal) || _dependees.nonEmpty
    }

    def updateTACDependee(tacDependee: EOptionP[Method, TACAI]): Unit = {
        _tacDependee = Some(tacDependee)
    }

    def addPointsToDependency(depender: Entity, dependee: EOptionP[Entity, PointsTo]): Unit = {
        _dependeeToDependers.getOrElseUpdate(dependee.e, mutable.Set.empty) + depender
        _dependerToDependees.getOrElseUpdate(depender, mutable.Set.empty) + dependee.e
        _dependees(dependee.e) = dependee
    }

    def removeDependee(eps: SomeEPS): Unit = {
        val dependee = eps.e
        _dependees.remove(dependee)
        val dependers = _dependeeToDependers(dependee)
        _dependeeToDependers.remove(dependee)
        for (depender ← dependers) {
            val dependees = _dependerToDependees(depender)
            dependees - dependee
            if (dependees.isEmpty)
                _dependerToDependees.remove(depender)
        }
    }

    def updatePointsToDependee(eps: EOptionP[Entity, PointsTo]): Unit = {
        _dependees(eps.e) = eps
    }
}

object PointsToState {
    def apply(
        method:      DefinedMethod,
        tacDependee: EOptionP[Method, TACAI]
    ): PointsToState = {
        new PointsToState(method, Some(tacDependee))
    }
}

class AndersenStylePointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    private implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)
    private val formalParameters: VirtualFormalParameters = p.get(VirtualFormalParametersKey)
    private val definitionSites: DefinitionSites = p.get(DefinitionSitesKey)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        (propertyStore(declaredMethod, CallersProperty.key): @unchecked) match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] ⇒
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                }
            // the method is reachable, so we analyze it!
        }

        // we only allow defined methods
        if (!declaredMethod.hasSingleDefinedMethod)
            return NoResult;

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        val tacEP = propertyStore(method, TACAI.key)
        implicit val state: PointsToState = PointsToState(declaredMethod.asDefinedMethod, tacEP)

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined) {
            processMethod
        } else {
            InterimPartialResult(Some(tacEP), c(state))
        }
    }

    def c(state: PointsToState)(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case UBP(tacai: TACAI) if tacai.tac.isDefined ⇒
            state.updateTACDependee(eps.asInstanceOf[EPS[Method, TACAI]])
            processMethod(state)

        case UBP(_: TACAI) ⇒
            InterimPartialResult(Some(eps), c(state))

        case UBPS(pointsTo: PointsTo, isFinal) ⇒
            for (depender ← state._dependeeToDependers(eps.e)) {
                val newPointsToSet = state.pointsToSets(depender) ++ pointsTo.types
                state.pointsToSets(depender) = newPointsToSet

                if (isFinal)
                    state.removeDependee(eps)
                else
                    state.updatePointsToDependee(eps.asInstanceOf[EPS[Entity, PointsTo]])
            }

            returnResult(state)
    }

    @inline private[this] def toEntity(
        defSite: Int, stmts: Array[Stmt[DUVar[ValueInformation]]]
    )(implicit state: PointsToState): Entity = {
        if (defSite < 0) {
            formalParameters.apply(state.method)(-1 - defSite)
        } else {
            definitionSites(state.method.definedMethod, stmts(defSite).pc)
        }
    }

    // todo: rename
    // IMPROVE: use local information, if possible
    def handleEOptP(
        e: Entity, pointsToSetEOptP: EOptionP[Entity, PointsTo]
    )(implicit state: PointsToState): UIDSet[ObjectType] = {
        pointsToSetEOptP match {
            case UBPS(pointsTo, isFinal) ⇒
                if (!isFinal) state.addPointsToDependency(e, pointsToSetEOptP)
                pointsTo.types

            case _: EPK[Entity, PointsTo] ⇒
                state.addPointsToDependency(e, pointsToSetEOptP)
                UIDSet.empty
        }
    }

    // todo: rename
    def handleDefSites(e: Entity, defSites: IntTrieSet)(implicit state: PointsToState): Unit = {
        var pointsToSet = UIDSet.empty[ObjectType]
        for (defSite ← defSites) {
            pointsToSet ++=
                handleEOptP(e, ps(toEntity(defSite, state.tac.stmts), PointsTo.key))

        }

        state.pointsToSets(e) = pointsToSet
    }

    def processMethod(implicit state: PointsToState): ProperPropertyComputationResult = {
        val calleesEOptP = ps(state.method, Callees.key)
        val callees = calleesEOptP.ub
        val tac = state.tac
        val method = state.method.definedMethod

        for (stmt ← tac.stmts) stmt match {
            case Assignment(pc, _, New(_, t)) ⇒
                val defSite = definitionSites(method, pc)
                state.pointsToSets(defSite) = UIDSet(t)

            case Assignment(pc, targetVar, UVar(_, defSites)) if targetVar.value.isReferenceValue ⇒
                val defSiteObject = definitionSites(method, pc)
                handleDefSites(defSiteObject, defSites)

            case Assignment(pc, targetVar, GetField(_, declaringClass, name, fieldType, _)) if targetVar.value.isReferenceValue ⇒
                val defSiteObject = definitionSites(method, pc)
                // todo: we assert that the field exists
                val field = p.resolveFieldReference(declaringClass, name, fieldType).get
                state.pointsToSets(defSiteObject) = handleEOptP(defSiteObject, ps(field, PointsTo.key))

            case Assignment(pc, targetVar, GetStatic(_, declaringClass, name, fieldType)) if targetVar.value.isReferenceValue ⇒
                val defSiteObject = definitionSites(method, pc)
                // todo: we assert that the field exists
                val field = p.resolveFieldReference(declaringClass, name, fieldType).get
                state.pointsToSets(defSiteObject) = handleEOptP(defSiteObject, ps(field, PointsTo.key))

            case Assignment(pc, targetVar, ArrayLoad(_, _, arrayRef)) if targetVar.value.isReferenceValue ⇒
                val defSiteObject = definitionSites(method, pc)
                val arrayBaseType = ObjectType.Object // todo: arrayRef.asVar.value.asReferenceValue.upperTypeBound
                state.pointsToSets(defSiteObject) = handleEOptP(defSiteObject, ps(arrayBaseType, PointsTo.key))

            case Assignment(pc, targetVar, call: FunctionCall[DUVar[ValueInformation]]) ⇒
                val targets = callees.callees(pc)
                val defSiteObject = definitionSites(method, pc)

                if (targetVar.value.isReferenceValue) {
                    var pointsToSet = UIDSet.empty[ObjectType]
                    for (target ← targets) {
                        pointsToSet ++= handleEOptP(defSiteObject, ps(target, PointsTo.key))
                    }

                    state.pointsToSets(defSiteObject, pointsToSet)
                }

                for (target ← targets) {
                    val fps = formalParameters(target)

                    val receiverOpt = call.receiverOption
                    // handle receiver for non static methods
                    if (receiverOpt.isDefined) {
                        val fp = fps(0)
                        handleDefSites(fp, receiverOpt.get.asVar.definedBy)
                    }

                    // handle params
                    for (i ← 0 until target.descriptor.parametersCount) {
                        val fp = fps(i + 1)
                        handleDefSites(fp, call.params(i).asVar.definedBy)
                    }
                }

            case Assignment(_, targetVar, _) if targetVar.value.isReferenceValue ⇒
                throw new IllegalArgumentException(s"unexpected assignment: $stmt")

            case call: Call[DUVar[ValueInformation]] ⇒
                val targets = callees.callees(call.pc)
                for (target ← targets) {
                    val fps = formalParameters(target)

                    val receiverOpt = call.receiverOption
                    // handle receiver for non static methods
                    if (receiverOpt.isDefined) {
                        val fp = fps(0)
                        handleDefSites(fp, receiverOpt.get.asVar.definedBy)
                    }

                    // handle params
                    for (i ← 0 until target.descriptor.parametersCount) {
                        val fp = fps(i + 1)
                        handleDefSites(fp, call.params(i).asVar.definedBy)
                    }
                }

            // todo we need the base type of the array
            case ArrayStore(_, _, _, value @ UVar(_, defSites)) if value.value.isReferenceValue ⇒
                val arrayBaseType = ObjectType.Object // todo: value.asVar.value.asReferenceValue.upperTypeBound

                var pointsToSet = UIDSet.empty[ObjectType]
                for (defSite ← defSites) {
                    pointsToSet ++=
                        handleEOptP(arrayBaseType, ps(toEntity(defSite, tac.stmts), PointsTo.key))

                }

                state.pointsToSets(arrayBaseType) = pointsToSet

            case PutField(_, declaringClass, name, fieldType, _, UVar(_, defSites)) if fieldType.isObjectType ⇒
                val field = p.resolveFieldReference(declaringClass, name, fieldType).get
                var pointsToSet = UIDSet.empty[ObjectType]
                for (defSite ← defSites) {
                    pointsToSet ++=
                        handleEOptP(field, ps(toEntity(defSite, tac.stmts), PointsTo.key))
                }
                state.pointsToSets(field) = pointsToSet

            case PutStatic(_, declaringClass, name, fieldType, UVar(_, defSites)) if fieldType.isObjectType ⇒
                val field = p.resolveFieldReference(declaringClass, name, fieldType).get
                var pointsToSet = UIDSet.empty[ObjectType]
                for (defSite ← defSites) {
                    pointsToSet ++=
                        handleEOptP(field, ps(toEntity(defSite, tac.stmts), PointsTo.key))
                }
                state.pointsToSets(field) = pointsToSet

            case ReturnValue(_, value @ UVar(_, defSites)) if value.value.isReferenceValue ⇒
                var pointsToSet = UIDSet.empty[ObjectType]
                for (defSite ← defSites) {
                    pointsToSet ++=
                        handleEOptP(state.method, ps(toEntity(defSite, tac.stmts), PointsTo.key))
                }
                state.pointsToSets(state.method) = pointsToSet

            case _ ⇒
        }

        returnResult
    }

    def returnResult(implicit state: PointsToState): ProperPropertyComputationResult = {
        val results = ArrayBuffer.empty[ProperPropertyComputationResult]

        if (state.hasOpenDependees) results += InterimPartialResult(state.dependees, c(state))

        for ((e, pointsToSet) ← state.pointsToSets) {
            val isFinal = !state._dependerToDependees.contains(e)
            results += PartialResult[Entity, PointsTo](e, PointsTo.key, {
                case _: EPK[Entity, PointsTo] if isFinal ⇒
                    Some(FinalP(e, PointsTo(pointsToSet)))

                case _: EPK[Entity, PointsTo] ⇒
                    Some(InterimEUBP(e, PointsTo(pointsToSet)))

                case UBP(ub) ⇒
                    // IMPROVE: only process new Types
                    val newPointsTo = ub.updated(pointsToSet)
                    if (newPointsTo.numElements != ub.numElements)
                        if (isFinal)
                            Some(FinalP(e, newPointsTo))
                        else
                            Some(InterimEUBP(e, newPointsTo))
                    else
                        None
            })
        }

        Results(results)
    }
}

object AndersenStylePointsToAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        CallersProperty,
        Callees,
        PointsTo,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(PointsTo)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): AndersenStylePointsToAnalysis = {
        val analysis = new AndersenStylePointsToAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}
