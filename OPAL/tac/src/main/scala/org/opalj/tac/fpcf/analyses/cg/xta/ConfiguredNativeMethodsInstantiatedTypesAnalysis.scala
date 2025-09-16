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
import org.opalj.tac.fpcf.analyses.EntityAssignment
import org.opalj.tac.fpcf.analyses.MethodDescription
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

    private[this] val nativeMethodData: Map[DeclaredMethod, Array[EntityAssignment]] =
        ConfiguredMethods
            .reader
            .read(p.config, "org.opalj.fpcf.analyses.ConfiguredNativeMethodsAnalysis")
            .nativeMethods
            .filter(_.pointsTo.isDefined)
            .map { v => (v.method, v.pointsTo.get) }
            .toMap

    override def processMethodWithoutBody(callContext: ContextType): ProperPropertyComputationResult = {
        val declaredMethod = callContext.method

        if (!nativeMethodData.contains(declaredMethod)) {
            // We have nothing to contribute to this method
            return Results()
        }

        val configuredData = nativeMethodData(declaredMethod)
        // Method may be without body (native) or not - we want both to work
        val typeSetEntity = typeSetEntitySelector(declaredMethod)
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

    override def processMethod(callContext: ContextType, tacEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        processMethodWithoutBody(callContext)
    }

    /**
     * Handles the effect of configured type instantiations on the current method
     * @param state Current method's state
     * @param partialResults Current partial results
     */
    private def processStaticConfigurations(implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        state.configurationData.foreach {
            case EntityAssignment(StaticFieldDescription(cf, name, fieldType), asd: AllocationSiteDescription) =>
                // This means an instantiated object is configured to be assigned to a static field. We want to add the
                // instantiated type to the field's TypeSetEntity only if it matches the field type.
                val theField = declaredFields(ClassType(cf), name, FieldType(fieldType))
                val allocatedType = FieldType(asd.instantiatedType)

                val fieldSetEntity = typeSetEntitySelector(theField)

                // Check that the field and the configured instantiated type are Reference Types and that they are compatible
                if (allocatedType.isReferenceType && theField.fieldType.isReferenceType &&
                    candidateMatchesTypeFilter(allocatedType.asReferenceType, theField.fieldType.asReferenceType)
                ) {
                    // Update the set of instantiated types for the field's TypeSetEntity
                    partialResults += PartialResult[TypeSetEntity, InstantiatedTypes](
                        fieldSetEntity,
                        InstantiatedTypes.key,
                        InstantiatedTypes.update(fieldSetEntity, UIDSet(allocatedType.asReferenceType))
                    )
                } else {
                    // Issue a warning if the configured types are not compatible
                    OPALLogger.warn(
                        "project configuration",
                        s"configured points to data is invalid for ${state.callContext.method.toJava}"
                    )
                }

            case EntityAssignment(MethodDescription(cf, name, desc), asd: AllocationSiteDescription) =>
                // This means an object instantiation is configured to be the return value of a method - this means the
                // instantiation happens inside the method. We must thus assign the instantiated type to the method's
                // TypeSetEntity
                assignInstantiationToMethod(cf, name, desc, asd)

            case EntityAssignment(ParameterDescription(cf, name, desc, _), asd: AllocationSiteDescription) =>
                // This means an object instantiated is configured to be the parameter in a method invocation - this
                // means the instantiation happens inside the calling method. We must thus assign the instantiated type
                // to the calling method's TypeSetEntity
                assignInstantiationToMethod(cf, name, desc, asd)

            case _ =>
        }
    }

    private def assignInstantiationToMethod(cf: String, name: String, desc: String, asd: AllocationSiteDescription)(
        implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        val theMethod = state.callContext.method

        // Assert that the method description matches the current method - we cannot assign instantiations to
        // a different method.
        if (theMethod.declaringClassType.fqn != cf || theMethod.name != name || theMethod.descriptor.toJVMDescriptor != desc) {
            OPALLogger.warn(
                "project configuration",
                s"configured points to data is invalid for ${theMethod.toJava}"
            )
        } else {
            val allocatedType = FieldType(asd.instantiatedType)
            val methodSetEntity = state.typeSetEntity

            // We only allow ReferenceTypes to be configured
            if (allocatedType.isReferenceType) {

                // Update the set of instantiated types for this method's TypeSetEntity
                partialResults += PartialResult[TypeSetEntity, InstantiatedTypes](
                    methodSetEntity,
                    InstantiatedTypes.key,
                    InstantiatedTypes.update(methodSetEntity, UIDSet(allocatedType.asReferenceType))
                )
            }
        }
    }

    /**
     * Handles the effects of parameters being assigned to relevant method entities via the configuration
     * @param typesToConsider The types that are considered to be instantiated for the current method's TypeSetEntity
     * @param state The current method's state
     * @param partialResults The partial results buffer
     */
    private def processParameterAssignments(typesToConsider: UIDSet[ReferenceType])(implicit
        state:          State,
        partialResults: ArrayBuffer[SomePartialResult]
    ): Unit = {
        state.configurationData.foreach {

            case EntityAssignment(StaticFieldDescription(cf, name, fieldType), pd: ParameterDescription) =>
                // This means that a parameter is configured to be assigned to a static field via this method. We need
                // to assign all methods that are instantiated for this method's TypeSetEntity **and** that are compatible
                // to the parameter type to the static field's TypeSetEntity.

                val theField = declaredFields(ClassType(cf), name, FieldType(fieldType))
                val fieldSetEntity = typeSetEntitySelector(theField)
                val theParameter = pd.fp(state.callContext.method, virtualFormalParameters)

                // Obtain the parameter type
                val theParameterType = if (theParameter.origin == -1) {
                    ClassType(pd.cf)
                } else {
                    val paramIdx = -theParameter.origin - 2
                    state.callContext.method.descriptor.parameterType(paramIdx)
                }

                // Check that the parameter type is a ReferenceType that matches the field's type
                if (theField.fieldType.isReferenceType && theParameterType.isReferenceType &&
                    candidateMatchesTypeFilter(theParameterType.asReferenceType, theField.fieldType.asReferenceType)
                ) {
                    // Obtain those types that are instantiated in the current context (typeToConsider) and compatible
                    // to the static field's type. Those types are possible values to add for the field.
                    val filteredTypes =
                        typesToConsider.foldLeft(UIDSet.newBuilder[ReferenceType]) { (builder, newType) =>
                            if (candidateMatchesTypeFilter(newType, theParameterType.asReferenceType)) {
                                builder += newType
                            }
                            builder
                        }.result()

                    // Update the instantiated types property of the field's TypeSetEntity
                    partialResults += PartialResult[TypeSetEntity, InstantiatedTypes](
                        fieldSetEntity,
                        InstantiatedTypes.key,
                        InstantiatedTypes.update(fieldSetEntity, filteredTypes)
                    )
                } else {
                    // Issue warning about incompatible type in configuration
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
