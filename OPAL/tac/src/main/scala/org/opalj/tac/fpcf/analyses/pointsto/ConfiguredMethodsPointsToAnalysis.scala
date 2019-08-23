/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
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
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.ArrayType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.tac.common.DefinitionSitesKey

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
    private lazy val virtualFormalParameters = project.get(VirtualFormalParametersKey)

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

        if (nativeMethodData.contains(dm) && nativeMethodData(dm).nonEmpty)
            handleNativeMethod(dm.asDefinedMethod, nativeMethodData(dm).get)
        else
            NoResult
    }

    private[this] def handleNativeMethod(
        dm:   DefinedMethod,
        data: Array[PointsToRelation]
    ): PropertyComputationResult = {
        implicit val state: State = new PointsToAnalysisState[ElementType, PointsToSet](dm, null)

        var nextPC = 1
        // for each configured points to relation, add all points-to info from the rhs to the lhs
        for (PointsToRelation(lhs, rhs) ← data) {
            nextPC = handleGet(rhs, 0, nextPC)
            nextPC = handlePut(lhs, 0, nextPC)
        }

        Results(createResults(state))
    }

    @inline override protected[this] def toEntity(defSite: Int)(implicit state: State): Entity = {
        definitionSites(state.method.definedMethod, defSite)
    }

    private[this] def handleGet(
        rhs: EntityDescription, pc: Int, nextPC: Int
    )(implicit state: State): Int = {
        val defSiteObject = definitionSites(state.method.definedMethod, pc)
        rhs match {
            case md: MethodDescription ⇒
                val method = md.method(declaredMethods)
                state.includeSharedPointsToSet(
                    defSiteObject,
                    currentPointsTo(defSiteObject, method, PointsToSetLike.noFilter),
                    PointsToSetLike.noFilter
                )

            case sfd: StaticFieldDescription ⇒
                val fieldOption = sfd.fieldOption(p)
                if (fieldOption.isDefined)
                    handleGetStatic(fieldOption.get, pc, checkForCast = false)

            case pd: ParameterDescription ⇒
                val method = pd.method(declaredMethods)
                val fp = pd.fp(method, virtualFormalParameters)
                if (fp ne null) {
                    state.includeSharedPointsToSet(
                        defSiteObject,
                        currentPointsTo(defSiteObject, fp, PointsToSetLike.noFilter),
                        PointsToSetLike.noFilter
                    )
                }

            case asd: AllocationSiteDescription ⇒
                val method = asd.method(declaredMethods)
                if (asd.instantiatedType.startsWith("[")) {
                    val theInstantiatedType = FieldType(asd.instantiatedType).asArrayType
                    val pts =
                        createPointsToSet(pc, method, theInstantiatedType, isConstant = false)
                    state.includeSharedPointsToSet(defSiteObject, pts, PointsToSetLike.noFilter)
                    if (asd.arrayComponentTypes.nonEmpty) {
                        var arrayEntity: ArrayEntity[ElementType] = null // TODO ugly hack
                        pts.forNewestNElements(1)(as ⇒ arrayEntity = ArrayEntity(as))
                        var arrayPTS: PointsToSet = emptyPointsToSet
                        asd.arrayComponentTypes.foreach { componentTypeString ⇒
                            val componentType = ObjectType(componentTypeString)
                            arrayPTS = arrayPTS.included(
                                createPointsToSet(pc, method, componentType, isConstant = false)
                            )
                        }
                        state.includeSharedPointsToSet(
                            arrayEntity, arrayPTS, PointsToSetLike.noFilter
                        )
                    }
                } else {
                    val theInstantiatedType = ObjectType(asd.instantiatedType)
                    state.includeSharedPointsToSet(
                        defSiteObject,
                        createPointsToSet(pc, method, theInstantiatedType, isConstant = false),
                        PointsToSetLike.noFilter
                    )
                }

            case ArrayDescription(array, arrayType) ⇒
                val arrayPC = nextPC
                val theNextPC = handleGet(array, arrayPC, nextPC) + 1
                handleArrayLoad(
                    ArrayType(ObjectType(arrayType)), pc, IntTrieSet(arrayPC), checkForCast = false
                )
                return theNextPC;
        }
        nextPC
    }

    private[this] def handlePut(
        lhs: EntityDescription, pc: Int, nextPC: Int
    )(implicit state: State): Int = {
        lhs match {
            case md: MethodDescription ⇒
                val method = md.method(declaredMethods)
                val returnType = method.descriptor.returnType.asReferenceType
                val filter = { t: ReferenceType ⇒
                    classHierarchy.isSubtypeOf(t, returnType)
                }
                state.includeSharedPointsToSet(
                    method,
                    currentPointsToOfDefSite(method, 0, filter),
                    filter
                )

            case sfd: StaticFieldDescription ⇒
                val fieldOption = sfd.fieldOption(p)
                if (fieldOption.isDefined)
                    handlePutStatic(fieldOption.get, IntTrieSet(0))

            case pd: ParameterDescription ⇒
                val method = pd.method(declaredMethods)
                val fp = pd.fp(method, virtualFormalParameters)
                if (fp ne null) {
                    if (fp.origin == -1) {
                        handleCallReceiver(IntTrieSet(0), method, isNonVirtualCall = true)
                    } else {
                        handleCallParameter(IntTrieSet(0), -fp.origin - 2, method)
                    }
                }

            case _: AllocationSiteDescription ⇒
                throw new RuntimeException("AllocationSites must not be assigned to")

            case ArrayDescription(array, arrayType) ⇒
                val arrayPC = nextPC
                val theNextPC = handleGet(array, arrayPC, nextPC) + 1
                handleArrayStore(
                    ArrayType(ObjectType(arrayType)), IntTrieSet(arrayPC), IntTrieSet(0)
                )
                return theNextPC;
        }
        nextPC
    }
}

trait ConfiguredMethodsPointsToAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    def propertyKind: PropertyMetaInformation
    def createAnalysis: SomeProject ⇒ ConfiguredMethodsPointsToAnalysis

    override type InitializationData = Null

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey)

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
        val analysis = createAnalysis(p)
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
    override val createAnalysis: SomeProject ⇒ ConfiguredMethodsPointsToAnalysis =
        new TypeBasedConfiguredMethodsPointsToAnalysis(_)
}

class AllocationSiteBasedConfiguredMethodsPointsToAnalysis private[analyses] (
    final val project: SomeProject
) extends ConfiguredMethodsPointsToAnalysis with AllocationSiteBasedAnalysis

object AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler
        extends ConfiguredMethodsPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject ⇒ ConfiguredMethodsPointsToAnalysis =
        new AllocationSiteBasedConfiguredMethodsPointsToAnalysis(_)
}