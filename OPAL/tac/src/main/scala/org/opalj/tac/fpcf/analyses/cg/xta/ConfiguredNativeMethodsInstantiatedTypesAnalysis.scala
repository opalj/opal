/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import scala.collection.mutable.ArrayBuffer

import org.opalj.br.ClassType
import org.opalj.br.DeclaredMethod
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomePartialResult
import org.opalj.log.OPALLogger
import org.opalj.tac.fpcf.analyses.AllocationSiteDescription
import org.opalj.tac.fpcf.analyses.ConfiguredMethods
import org.opalj.tac.fpcf.analyses.MethodDescription
import org.opalj.tac.fpcf.analyses.PointsToRelation
import org.opalj.tac.fpcf.analyses.StaticFieldDescription
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Handles the effect of certain (configured native methods) to the set of instantiated types.
 *
 * @author Johannes Düsing
 */
class ConfiguredNativeMethodsInstantiatedTypesAnalysis private[analyses] (
    final val project:               SomeProject,
    final val typeSetEntitySelector: TypeSetEntitySelector
) extends ReachableMethodAnalysis {

    private[this] val declaredFields: DeclaredFields = p.get(DeclaredFieldsKey)
    private[this] val virtualFormalParameters = project.get(VirtualFormalParametersKey)

    private type State = ConfiguredNativeMethodsTypePropagationState[ContextType]

    // TODO remove dependency to classes in pointsto package
    private[this] val nativeMethodData: Map[DeclaredMethod, Array[PointsToRelation]] =
        ConfiguredMethods
            .reader
            .read(p.config, "org.opalj.fpcf.analyses.ConfiguredNativeMethodsAnalysis")
            .nativeMethods
            .filter(_.pointsTo.isDefined)
            .map { v => (v.method, v.pointsTo.get) }
            .toMap

    override def processMethodWithoutBody(callContext: ContextType): ProperPropertyComputationResult = {
        processMethodInternal(callContext)
    }

    override def processMethod(callContext: ContextType, tacEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        processMethodInternal(callContext)
    }

    private def processMethodInternal(callContext: ContextType): ProperPropertyComputationResult = {
        if (!nativeMethodData.contains(callContext.method)) {
            // We have nothing to contribute to this method
            return Results()
        }

        val configuredData = nativeMethodData(callContext.method)
        // Method may be without body (native) or not - we want both to work
        val typeSetEntity = typeSetEntitySelector(callContext.method)
        val instantiatedTypesEOptP = propertyStore(typeSetEntity, InstantiatedTypes.key)

        implicit val state: ConfiguredNativeMethodsTypePropagationState[ContextType] =
            new ConfiguredNativeMethodsTypePropagationState(
                callContext,
                configuredData,
                typeSetEntity,
                instantiatedTypesEOptP
            )

        implicit val partialResults: ArrayBuffer[SomePartialResult] = ArrayBuffer.empty[SomePartialResult]

        processParameterAssignments(state.ownInstantiatedTypes)
        processStaticConfigurations

        returnResults(partialResults)
    }

    private def processStaticConfigurations(implicit state: State, partialResults: ArrayBuffer[SomePartialResult]): Unit = {
        state.configurationData.foreach {
            case PointsToRelation(StaticFieldDescription(cf, name, fieldType), asd: AllocationSiteDescription) =>
                val theField = declaredFields(ClassType(cf), name, FieldType(fieldType))
                val allocatedType = FieldType(asd.instantiatedType)

                val fieldSetEntity = typeSetEntitySelector(theField)

                if (allocatedType.isReferenceType && theField.fieldType.isReferenceType &&
                    candidateMatchesTypeFilter(allocatedType.asReferenceType, theField.fieldType.asReferenceType)
                ) {
                    partialResults += PartialResult[TypeSetEntity, InstantiatedTypes](
                        fieldSetEntity,
                        InstantiatedTypes.key,
                        InstantiatedTypes.update(fieldSetEntity, UIDSet(allocatedType.asReferenceType))
                    )
                } else {
                    OPALLogger.warn(
                        "project configuration",
                        s"configured points to data is invalid for ${state.callContext.method.toJava}"
                    )
                }

            case PointsToRelation(MethodDescription(cf, name, desc), asd: AllocationSiteDescription) =>
                val theMethod = state.callContext.method

                if (
                    theMethod.declaringClassType.fqn != cf || theMethod.name != name || theMethod.descriptor.toJVMDescriptor != desc
                ) {
                    OPALLogger.warn(
                        "project configuration",
                        s"configured points to data is invalid for ${state.callContext.method.toJava}"
                    )
                } else {
                    val allocatedType = FieldType(asd.instantiatedType)
                    val methodSetEntity = state.typeSetEntity

                    if (allocatedType.isReferenceType) {
                        partialResults += PartialResult[TypeSetEntity, InstantiatedTypes](
                            methodSetEntity,
                            InstantiatedTypes.key,
                            InstantiatedTypes.update(methodSetEntity, UIDSet(allocatedType.asReferenceType))
                        )
                    }
                }

            case _ =>
        }
    }

    private def processParameterAssignments(typesToConsider: UIDSet[ReferenceType])(implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        state.configurationData.foreach {

            case PointsToRelation(StaticFieldDescription(cf, name, fieldType), pd: ParameterDescription) =>
                val theField = declaredFields(ClassType(cf), name, FieldType(fieldType))
                val fieldSetEntity = typeSetEntitySelector(theField)
                val theParameter = pd.fp(state.callContext.method, virtualFormalParameters)

                val theParameterType = if (theParameter.origin == -1) {
                    ClassType(pd.cf)
                } else {
                    val paramIdx = -theParameter.origin - 2
                    state.callContext.method.descriptor.parameterType(paramIdx)
                }

                if (theField.fieldType.isReferenceType && theParameterType.isReferenceType &&
                    candidateMatchesTypeFilter(theParameterType.asReferenceType, theField.fieldType.asReferenceType)
                ) {
                    val filteredTypes =
                        typesToConsider.foldLeft(UIDSet.newBuilder[ReferenceType]) { (builder, newType) =>
                            if (candidateMatchesTypeFilter(newType, theParameterType.asReferenceType)) {
                                builder += newType
                            }
                            builder
                        }.result()

                    partialResults += PartialResult[TypeSetEntity, InstantiatedTypes](
                        fieldSetEntity,
                        InstantiatedTypes.key,
                        InstantiatedTypes.update(fieldSetEntity, filteredTypes)
                    )
                } else {
                    OPALLogger.warn(
                        "project configuration",
                        s"configured points to data is invalid for ${state.callContext.method.toJava}"
                    )
                }
            case _ =>
        }
    }

    private def returnResults(partialResults: IterableOnce[SomePartialResult])(implicit
        state: State
    ): ProperPropertyComputationResult = {
        // Always re-register the continuation. It is impossible for all dependees to be final in XTA/...
        Results(
            InterimPartialResult(state.dependees, c(state)),
            partialResults
        )
    }

    private def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = eps match {

        case EUBP(e: TypeSetEntity, _: InstantiatedTypes) if e == state.typeSetEntity =>
            val theEPS = eps.asInstanceOf[EPS[TypeSetEntity, InstantiatedTypes]]

            val previouslySeenTypes = state.ownInstantiatedTypes.size
            state.updateOwnInstantiatedTypesDependee(theEPS)
            val unseenTypes = UIDSet(theEPS.ub.dropOldest(previouslySeenTypes).toSeq: _*)

            implicit val partialResults: ArrayBuffer[SomePartialResult] = ArrayBuffer.empty[SomePartialResult]

            processParameterAssignments(unseenTypes)(state, partialResults)

            returnResults(partialResults)(state)
        case _ =>
            sys.error("received unexpected update")
    }

    // Taken from rta TypePropagationAnalysis
    private def candidateMatchesTypeFilter(candidateType: ReferenceType, filterType: ReferenceType): Boolean = {
        val answer = classHierarchy.isASubtypeOf(candidateType, filterType)

        if (answer.isYesOrNo) {
            // Here, we know for sure that the candidate type is or is not a subtype of the filter type.
            answer.isYes
        } else {
            // If the answer is Unknown, we don't know for sure whether the candidate is a subtype of the filter type.
            // However, ClassHierarchy returns Unknown even for cases where it is very unlikely that this is the case.
            // Therefore, we take some more features into account to make the filtering more precise.

            // Important: This decision is a possible but unlikely cause of unsoundness in the call graph!

            // If the filter type is not a project type (i.e., it is external), we assume that any candidate type
            // is a subtype. This can be any external type or project types for which we have incomplete supertype
            // information.
            // If the filter type IS a project type, we consider the candidate type not to be a subtype since this is
            // very likely to be not the case. For the candidate type, there are two options: Either it is an external
            // type, in which case the candidate type could only be a subtype if project types are available in the
            // external type's project at compile time. This is very unlikely since external types are almost always
            // from libraries (like the JDK) which are not available in the analysis context, and which were almost
            // certainly compiled separately ("Separate Compilation Assumption").
            // The other option is that the candidate is also a project type, in which case we should have gotten a
            // definitive Yes/No answer before. Since we didn't get one, the candidate type probably has a supertype
            // which is not a project type. In that case, the above argument applies similarly.

            val filterTypeIsProjectType = if (filterType.isClassType) {
                project.isProjectType(filterType.asClassType)
            } else {
                val at = filterType.asArrayType
                project.isProjectType(at.elementType.asClassType)
            }

            !filterTypeIsProjectType
        }
    }
}

class ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler(setEntitySelector: TypeSetEntitySelector)
    extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, DeclaredFieldsKey, VirtualFormalParametersKey)

    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, TACAI, InstantiatedTypes)

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(InstantiatedTypes)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): ConfiguredNativeMethodsInstantiatedTypesAnalysis = {
        val analysis = new ConfiguredNativeMethodsInstantiatedTypesAnalysis(p, setEntitySelector)

        ps.registerTriggeredComputation(Callers.key, analysis.analyze)

        analysis
    }

    override def triggeredBy: PropertyKind = Callers.key

}
