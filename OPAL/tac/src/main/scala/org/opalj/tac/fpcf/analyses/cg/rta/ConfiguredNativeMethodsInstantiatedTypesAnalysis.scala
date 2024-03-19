/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package rta

import scala.collection.immutable.ArraySeq

import org.opalj.br.ArrayType
import org.opalj.br.DeclaredMethod
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore

/**
 * Handles the effect of certain (configured native methods) to the set of instantiated types.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
class ConfiguredNativeMethodsInstantiatedTypesAnalysis private[analyses] (
    final val project: SomeProject
) extends FPCFAnalysis {

    private[this] implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)

    // TODO remove dependency to classes in pointsto package
    private[this] val nativeMethodData: Map[DeclaredMethod, Option[Array[PointsToRelation]]] = {
        ConfiguredMethods.reader
            .read(p.config, "org.opalj.fpcf.analyses.ConfiguredNativeMethodsAnalysis")
            .nativeMethods.map { v => (v.method, v.pointsTo) }.toMap
    }

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ReferenceType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] => eps.ub.types
            case _              => UIDSet.empty
        }
    }

    private[this] def canBeInstantiated(rt: ReferenceType): Boolean = rt match {
        case _: ArrayType => true
        case ot: ObjectType =>
            val cfOption = project.classFile(ot)
            cfOption.isDefined && {
                val cf = cfOption.get
                !cf.isInterfaceDeclaration && !cf.isAbstract
            }
    }

    def analyze(dm: DeclaredMethod): PropertyComputationResult = {
        (propertyStore(dm, Callers.key): @unchecked) match {
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

        val instantiatedTypes = if (nativeMethodData.contains(dm)) {
            val dataO = nativeMethodData(dm)
            if (dataO.isEmpty)
                return NoResult;
            dataO.get.collect {
                case PointsToRelation(_, as: AllocationSiteDescription) =>
                    as.arrayComponentTypes.map(ReferenceType(_)) :+
                        FieldType(as.instantiatedType).asReferenceType
            }.flatten
        } else if (dm.hasSingleDefinedMethod && dm.definedMethod.body.isEmpty &&
                   dm.descriptor.returnType.isReferenceType
        ) {
            val m = dm.definedMethod
            val returnType = m.returnType.asReferenceType
            // TODO We should probably handle ArrayTypes as well
            val types =
                if (m.returnType.isArrayType && m.returnType.asArrayType.elementType.isObjectType)
                    Array(returnType, m.returnType.asArrayType.elementType.asObjectType)
                else Array(returnType)
            types.filter(canBeInstantiated)
        } else
            return NoResult;

        val instantiatedTypesUB =
            getInstantiatedTypesUB(propertyStore(project, InstantiatedTypes.key))

        val newInstantiatedTypes = UIDSet(
            ArraySeq.unsafeWrapArray(instantiatedTypes.filterNot(instantiatedTypesUB.contains)): _*
        )

        if (newInstantiatedTypes.isEmpty)
            return NoResult;

        PartialResult(
            p,
            InstantiatedTypes.key,
            InstantiatedTypesAnalysis.update(p, newInstantiatedTypes)
        )
    }
}

object ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callers, InstantiatedTypes)

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(InstantiatedTypes)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): ConfiguredNativeMethodsInstantiatedTypesAnalysis = {
        val analysis = new ConfiguredNativeMethodsInstantiatedTypesAnalysis(p)

        ps.registerTriggeredComputation(Callers.key, analysis.analyze)

        analysis
    }

    override def triggeredBy: PropertyKind = Callers
}
