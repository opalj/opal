/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable.ArrayBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBPS
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.ObjectType
import org.opalj.br.DefinedMethod
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.FieldType
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet

/**
 * Applies the impact of preconfigured methods to the points-to analysis.
 *
 * TODO: example
 * TODO: refer to the config file
 *
 * @author Florian Kuebler
 */
trait ConfiguredMethodsPointsToAnalysis extends PointsToAnalysisBase {

    private[this] implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)

    private[this] val nativeMethodData: Map[DeclaredMethod, Option[Array[PointsToRelation]]] = {
        ConfiguredMethods.reader.read(
            p.config, "org.opalj.fpcf.analyses.ConfiguredNativeMethodsAnalysis"
        ).nativeMethods.map { v ⇒ (v.method, v.pointsTo) }.toMap
    }

    def analyze(dm: DeclaredMethod): PropertyComputationResult = {
        (propertyStore(dm, Callers.key): @unchecked) match {
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

        if (nativeMethodData.contains(dm)) {
            return handleNativeMethod(dm.asDefinedMethod);
        }

        NoResult
    }

    private[this] def handleNativeMethod(dm: DefinedMethod): PropertyComputationResult = {
        val nativeMethodDataOpt = nativeMethodData(dm)
        if (nativeMethodDataOpt.isEmpty)
            return NoResult;

        val data = nativeMethodDataOpt.get

        val results = ArrayBuffer.empty[ProperPropertyComputationResult]

        // for each configured points to relation, add all points-to info from the rhs to the lhs
        for (PointsToRelation(lhs, rhs) ← data) {
            rhs match {
                case asd: AllocationSiteDescription ⇒
                    if (asd.instantiatedType.startsWith("[")) {
                        val instantiatedType = FieldType(asd.instantiatedType).asArrayType
                        val pts = createPointsToSet(0, dm, instantiatedType, isConstant = false)
                        var as: ElementType = null// TODO ugly hack
                        pts.forNewestNElements(1)(as = _)
                        results += createPartialResultOpt(lhs.entity, pts).get
                        if (asd.arrayComponentTypes.nonEmpty) {
                            var arrayPTS: PointsToSet = emptyPointsToSet
                            asd.arrayComponentTypes.foreach { componentTypeString ⇒
                                val componentType = ObjectType(componentTypeString)
                                arrayPTS = arrayPTS.included(
                                    createPointsToSet(0, dm, componentType, isConstant = false)
                                )
                            }
                            results += createPartialResultOpt(ArrayEntity(as), arrayPTS).get
                        }
                    } else {
                        val instantiatedType = ObjectType(asd.instantiatedType)
                        val pts = createPointsToSet(0, dm, instantiatedType, isConstant = false)
                        results += createPartialResultOpt(lhs.entity, pts).get
                    }
                case _ ⇒
                    val pointsToEOptP = propertyStore(rhs.entity, pointsToPropertyKey)

                    // the points-to set associated with the rhs
                    val pts = if (pointsToEOptP.hasUBP)
                        pointsToEOptP.ub
                    else
                        emptyPointsToSet

                    // only create a partial result if there is some information to apply
                    // partial result that updates the points-to information
                    val prOpt = createPartialResultOpt(lhs.entity, pts)

                    // if the rhs is not yet final, we need to get updated if it changes
                    if (pointsToEOptP.isRefinable) {
                        results += InterimPartialResult(
                            prOpt, Some(pointsToEOptP), c(lhs.entity, pointsToEOptP)
                        )
                    } else if (prOpt.isDefined) {
                        results += prOpt.get
                    }
            }
        }
        Results(results)
    }

    private[this] def createPartialResultOpt(
        lhs:         Entity,
        newPointsTo: PointsToSet
    ): Option[PartialResult[Entity, PointsToSet]] = {
        if (newPointsTo.numTypes > 0) {
            Some(PartialResult[Entity, PointsToSet](lhs, pointsToPropertyKey, {
                case InterimUBP(ub: PointsToSet @unchecked) ⇒
                    // here we assert that updated returns the identity if pts is already contained
                    val newUB = ub.included(newPointsTo)
                    if (newUB eq ub) {
                        None
                    } else {
                        Some(InterimEUBP(lhs, newUB))
                    }
                case _: EPK[Entity, PointsToSet] ⇒
                    Some(InterimEUBP(lhs, newPointsTo))

                case fep: FinalEP[Entity, PointsToSet] ⇒
                    throw new IllegalStateException(s"unexpected final value $fep")
            }))
        } else
            None
    }

    private[this] def c(
        lhs: Entity, rhsEOptP: EOptionP[Entity, PointsToSet]
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case UBPS(rhsUB: PointsToSet @unchecked, rhsIsFinal) ⇒
            // there is no change, but still a dependency, just return this continuation
            if (rhsEOptP.hasUBP && (rhsEOptP.ub eq rhsUB) && eps.isRefinable) {
                InterimPartialResult(Some(eps), c(lhs, eps.asInstanceOf[EPS[Entity, PointsToSet]]))
            } else {
                val pr = PartialResult[Entity, PointsToSet](
                    lhs,
                    pointsToPropertyKey,
                    {
                        case InterimUBP(lhsUB: PointsToSet @unchecked) ⇒

                            // here we assert that updated returns the identity if pts is already contained
                            val newUB = lhsUB.included(rhsUB)
                            if (newUB eq lhsUB) {
                                None
                            } else {
                                Some(InterimEUBP(lhs, newUB))
                            }

                        case _: EPK[Entity, PointsToSet] ⇒
                            Some(InterimEUBP(lhs, rhsUB))

                        case fep: FinalEP[Entity, PointsToSet] ⇒
                            throw new IllegalStateException(s"unexpected final value $fep")
                    }
                )

                if (rhsIsFinal) {
                    pr
                } else {
                    InterimPartialResult(
                        Some(pr), Some(eps), c(lhs, eps.asInstanceOf[EPS[Entity, PointsToSet]])
                    )
                }
            }
        case _ ⇒
            throw new IllegalArgumentException(s"unexpected update $eps")
    }
}

trait ConfiguredMethodsPointsToAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    val propertyKind: PropertyMetaInformation
    val createAnalyis: SomeProject ⇒ ConfiguredMethodsPointsToAnalysis

    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        propertyKind
    )

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(propertyKind)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): ConfiguredMethodsPointsToAnalysis = {
        val analysis = createAnalyis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    /**
     * Specifies the kind of the properties that will trigger the analysis to be registered.
     */
    override def triggeredBy: PropertyKind = Callers
}

class TypeBasedConfiguredMethodsPointsToAnalysis private[analyses] (
    final val project: SomeProject
) extends ConfiguredMethodsPointsToAnalysis with TypeBasedAnalysis

object TypeBasedConfiguredMethodsPointsToAnalysisScheduler
        extends ConfiguredMethodsPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalyis: SomeProject ⇒ ConfiguredMethodsPointsToAnalysis =
        new TypeBasedConfiguredMethodsPointsToAnalysis(_)
}

class AllocationSiteBasedConfiguredMethodsPointsToAnalysis private[analyses] (
    final val project: SomeProject
) extends ConfiguredMethodsPointsToAnalysis with AllocationSiteBasedAnalysis

object AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler
    extends ConfiguredMethodsPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalyis: SomeProject ⇒ ConfiguredMethodsPointsToAnalysis =
        new AllocationSiteBasedConfiguredMethodsPointsToAnalysis(_)
}