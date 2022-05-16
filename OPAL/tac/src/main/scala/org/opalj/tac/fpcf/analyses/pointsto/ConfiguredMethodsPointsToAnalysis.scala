/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.FieldType
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.ArrayType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.SimpleContextProvider
import org.opalj.tac.fpcf.analyses.cg.TypeConsumerAnalysis

/**
 * Applies the impact of preconfigured methods to the points-to analysis.
 *
 * TODO: example
 * TODO: refer to the config file
 *
 * @author Florian Kuebler
 */
abstract class ConfiguredMethodsPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends PointsToAnalysisBase with TypeConsumerAnalysis {

    private[this] implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)
    private lazy val virtualFormalParameters = project.get(VirtualFormalParametersKey)

    private[this] val nativeMethodData: Map[DeclaredMethod, Option[Array[PointsToRelation]]] = {
        ConfiguredMethods.reader.read(
            p.config, "org.opalj.fpcf.analyses.ConfiguredNativeMethodsAnalysis"
        ).nativeMethods.map { v => (v.method, v.pointsTo) }.toMap
    }

    def analyze(dm: DeclaredMethod): PropertyComputationResult = {
        if (dm.isVirtualOrHasSingleDefinedMethod) { // FIXME Find way to do this even if no Method object exists

            val callers = propertyStore(dm, Callers.key)
            (callers: @unchecked) match {
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

            if (nativeMethodData.contains(dm) && nativeMethodData(dm).nonEmpty)
                handleCallers(callers, null, nativeMethodData(dm).get)
            else if (dm.hasSingleDefinedMethod && dm.definedMethod.body.isEmpty &&
                dm.descriptor.returnType.isReferenceType) {
                val m = dm.definedMethod
                val cf = m.classFile.thisType.toJVMTypeName
                val name = m.name
                val desc = m.descriptor.toJVMDescriptor
                val tpe = m.returnType.asReferenceType.toJVMTypeName
                val arrayTypes = if (m.returnType.isArrayType)
                    Seq(m.returnType.asArrayType.elementType.toJVMTypeName)
                else Seq.empty
                handleCallers(callers, null, Array(PointsToRelation(
                    MethodDescription(cf, name, desc),
                    AllocationSiteDescription(cf, name, desc, tpe, arrayTypes)
                )))
            } else
                NoResult
        } else
            NoResult
    }

    private[this] def handleCallers(
        newCallers: EOptionP[DeclaredMethod, Callers],
        oldCallers: Callers,
        data:       Array[PointsToRelation]
    ): ProperPropertyComputationResult = {
        val dm = newCallers.e
        var results: Iterator[ProperPropertyComputationResult] = Iterator.empty
        newCallers.ub.forNewCalleeContexts(oldCallers, dm) { callContext =>
            results ++= handleNativeMethod(callContext.asInstanceOf[ContextType], data)
        }
        if (newCallers.isRefinable) {
            results ++= Iterator(InterimPartialResult(
                Set(newCallers),
                (update: SomeEPS) => {
                    handleCallers(
                        update.asInstanceOf[EPS[DeclaredMethod, Callers]], newCallers.ub, data
                    )
                }
            ))
        }
        Results(results)
    }

    private[this] def handleNativeMethod(
        callContext: ContextType,
        data:        Array[PointsToRelation]
    ): Iterator[ProperPropertyComputationResult] = {
        implicit val state: State =
            new PointsToAnalysisState[ElementType, PointsToSet, ContextType](callContext, null)

        var pc = -1
        // for each configured points to relation, add all points-to info from the rhs to the lhs
        for (PointsToRelation(lhs, rhs) <- data) {
            val nextPC = handleGet(rhs, pc, pc - 1)
            pc = handlePut(lhs, pc, nextPC)
        }

        createResults(state).iterator
    }

    @inline override protected[this] def toEntity(defSite: Int)(implicit state: State): Entity = {
        getDefSite(defSite)
    }

    private[this] def handleGet(
        rhs: EntityDescription, pc: Int, nextPC: Int
    )(implicit state: State): Int = {
        val defSiteObject = getDefSite(pc)
        rhs match {
            case md: MethodDescription =>
                val method =
                    typeProvider.expandContext(state.callContext, md.method(declaredMethods), pc)
                state.includeSharedPointsToSet(
                    defSiteObject,
                    currentPointsTo(defSiteObject, method, PointsToSetLike.noFilter),
                    PointsToSetLike.noFilter
                )

            case sfd: StaticFieldDescription =>
                val fieldOption = sfd.fieldOption(p)
                if (fieldOption.isDefined)
                    handleGetStatic(fieldOption.get, pc, checkForCast = false)

            case pd: ParameterDescription =>
                val method = pd.method(declaredMethods)
                val fp = pd.fp(method, virtualFormalParameters)
                if (fp ne null) {
                    val entity = typeProvider match {
                        case _: SimpleContextProvider => fp
                        case _                        => (state.callContext, fp)
                    }
                    state.includeSharedPointsToSet(
                        defSiteObject,
                        currentPointsTo(defSiteObject, entity, PointsToSetLike.noFilter),
                        PointsToSetLike.noFilter
                    )
                }

            case asd: AllocationSiteDescription =>
                val method = asd.method(declaredMethods)
                val allocationContext = if (method == state.callContext.method) state.callContext
                else typeProvider.expandContext(state.callContext, method, pc)
                if (asd.instantiatedType.startsWith("[")) {
                    val theInstantiatedType = FieldType(asd.instantiatedType).asArrayType
                    val pts = createPointsToSet(
                        pc,
                        allocationContext,
                        theInstantiatedType,
                        isConstant = false
                    )
                    state.includeSharedPointsToSet(defSiteObject, pts, PointsToSetLike.noFilter)
                    if (asd.arrayComponentTypes.nonEmpty) {
                        val arrayEntity = ArrayEntity(pts.getNewestElement())
                        var arrayPTS: PointsToSet = emptyPointsToSet
                        asd.arrayComponentTypes.foreach { componentTypeString =>
                            val componentType = ObjectType(componentTypeString)
                            arrayPTS = arrayPTS.included(
                                createPointsToSet(
                                    pc,
                                    allocationContext,
                                    componentType,
                                    isConstant = false
                                )
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
                        createPointsToSet(
                            pc,
                            allocationContext,
                            theInstantiatedType,
                            isConstant = false
                        ),
                        PointsToSetLike.noFilter
                    )
                }

            case ArrayDescription(array, arrayType) =>
                val arrayPC = nextPC
                val theNextPC = handleGet(array, arrayPC, nextPC) - 1
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
            case md: MethodDescription =>
                val method = md.method(declaredMethods)
                val returnType = method.descriptor.returnType.asReferenceType
                val filter = { t: ReferenceType =>
                    classHierarchy.isSubtypeOf(t, returnType)
                }
                assert(method == state.callContext.method)
                val entity = state.callContext
                state.includeSharedPointsToSet(
                    entity,
                    currentPointsToOfDefSite(entity, pc, filter),
                    filter
                )

            case sfd: StaticFieldDescription =>
                val fieldOption = sfd.fieldOption(p)
                if (fieldOption.isDefined)
                    handlePutStatic(fieldOption.get, IntTrieSet(0))

            case pd: ParameterDescription =>
                val method = pd.method(declaredMethods)
                val fp = pd.fp(method, virtualFormalParameters)
                if (fp ne null) {
                    if (fp.origin == -1) {
                        handleCallReceiver(
                            IntTrieSet(pc),
                            typeProvider.expandContext(state.callContext, method, pc),
                            isNonVirtualCall = true
                        )
                    } else {
                        handleCallParameter(
                            IntTrieSet(pc),
                            -fp.origin - 2,
                            typeProvider.expandContext(state.callContext, method, pc)
                        )
                    }
                }

            case _: AllocationSiteDescription =>
                throw new RuntimeException("AllocationSites must not be assigned to")

            case ArrayDescription(array, arrayType) =>
                val arrayPC = nextPC
                val theNextPC = handleGet(array, arrayPC, nextPC) - 1
                handleArrayStore(
                    ArrayType(ObjectType(arrayType)), IntTrieSet(arrayPC), IntTrieSet(pc)
                )
                return theNextPC;
        }
        nextPC
    }
}

trait ConfiguredMethodsPointsToAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    def propertyKind: PropertyMetaInformation
    def createAnalysis: SomeProject => ConfiguredMethodsPointsToAnalysis

    override type InitializationData = Null

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey, TypeProviderKey)

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

object TypeBasedConfiguredMethodsPointsToAnalysisScheduler
    extends ConfiguredMethodsPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => ConfiguredMethodsPointsToAnalysis =
        new ConfiguredMethodsPointsToAnalysis(_) with TypeBasedAnalysis
}

object AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler
    extends ConfiguredMethodsPointsToAnalysisScheduler {

    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => ConfiguredMethodsPointsToAnalysis =
        new ConfiguredMethodsPointsToAnalysis(_) with AllocationSiteBasedAnalysis
}