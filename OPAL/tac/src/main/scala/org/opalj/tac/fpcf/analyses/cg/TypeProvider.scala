/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.annotation.nowarn
import scala.annotation.switch

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBPS
import org.opalj.value.IsMObjectValue
import org.opalj.value.IsNullValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.IsSReferenceValue
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet1
import org.opalj.br.ObjectType.ClassId
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong
import org.opalj.br.ObjectType.StringBufferId
import org.opalj.br.ObjectType.StringBuilderId
import org.opalj.br.ObjectType.StringId
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSites
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.NoInstantiatedTypes
import org.opalj.br.fpcf.properties.pointsto.NoTypes
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.fpcf.properties.pointsto.longToAllocationSite
import org.opalj.br.Field
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.xta.TypeSetEntity
import org.opalj.tac.fpcf.analyses.cg.xta.TypeSetEntitySelector
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.classConstPointsToSet
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergeClassConstsConfigKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergedPointsToSetForType
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergeExceptionsConfigKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergeStringBuilderBufferConfigKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergeStringConstsConfigKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.stringBufferPointsToSet
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.stringBuilderPointsToSet
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.stringConstPointsToSet
import org.opalj.tac.fpcf.analyses.pointsto.CallExceptions
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

/**
 * Core class of the call-graph framework: Provides type and (if available) points-to information to
 * client classes. Each type provider represents one traditional call-graph algorithm.
 *
 * Type providers are responsible for managing the dependencies for their internal information
 * themselves. They provide suitable continuation functions to be invoked from an analysis'
 * continuation in order to process these opaque dependencies.
 *
 * @author Dominik Helm
 */
trait TypeProvider {

    type ContextType <: Context
    type InformationType
    type PropertyType <: Property

    val usedPropertyKinds: Set[PropertyBounds]

    val project: SomeProject

    val providesAllocations: Boolean = false

    def typesProperty(
        use: V, context: ContextType, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeProviderState): InformationType

    def typesProperty(
        field: Field, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeProviderState
    ): InformationType

    def typesProperty(
        field: Field, fieldAllocation: DefinitionSite, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeProviderState
    ): InformationType = {
        typesProperty(field, depender)
    }

    def foreachType(
        use:             V,
        typesProperty:   InformationType,
        additionalTypes: Set[ReferenceType] = Set.empty
    )(handleType: ReferenceType ⇒ Unit): Unit

    def foreachType(
        field:         Field,
        typesProperty: InformationType
    )(handleType: ReferenceType ⇒ Unit): Unit

    def foreachAllocation(
        use: V, typesProperty: InformationType, additionalTypes: Set[ReferenceType] = Set.empty
    )(
        handleAllocation: (ReferenceType, ContextType, Int) ⇒ Unit
    ): Unit = {
        throw new UnsupportedOperationException
    }

    def foreachAllocation(
        field: Field, typesProperty: InformationType
    )(
        handleAllocation: (ReferenceType, ContextType, Int) ⇒ Unit
    ): Unit = {
        throw new UnsupportedOperationException
    }

    def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, PropertyType],
        additionalTypes: Set[ReferenceType]        = Set.empty
    )(
        handleNewType: ReferenceType ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        val epk = updatedEPS.toEPK
        val oldEOptP = state.getProperty(epk)

        continuation(use, updatedEPS, oldEOptP, additionalTypes, handleNewType)
    }

    def continuation(
        field:      Field,
        updatedEPS: EPS[Entity, Property]
    )(
        handleNewType: ReferenceType ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        val epk = updatedEPS.toEPK
        val oldEOptP = state.getProperty(epk)

        continuation(field, updatedEPS, oldEOptP, handleNewType)
    }

    @inline protected[this] def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, PropertyType],
        oldEOptP:        EOptionP[Entity, PropertyType],
        additionalTypes: Set[ReferenceType],
        handleNewType:   ReferenceType ⇒ Unit
    ): Unit

    @inline protected[this] def continuation(
        field:         Field,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType ⇒ Unit
    )(implicit state: TypeProviderState): Unit

    def continuationForAllocations(
        use:             V,
        updatedEPS:      EPS[Entity, PropertyType],
        additionalTypes: Set[ReferenceType]        = Set.empty
    )(
        handleNewAllocation: (ReferenceType, ContextType, Int) ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        val epk = updatedEPS.toEPK
        val oldEOptP = state.getProperty(epk)

        continuationForAllocations(use, updatedEPS, oldEOptP, additionalTypes, handleNewAllocation)
    }

    def continuationForAllocations(
        field:      Field,
        updatedEPS: EPS[Entity, Property]
    )(
        handleNewAllocation: (ReferenceType, ContextType, Int) ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        val epk = updatedEPS.toEPK
        val oldEOptP = state.getProperty(epk)

        continuationForAllocations(field, updatedEPS, oldEOptP, handleNewAllocation)
    }

    @inline protected[this] def continuationForAllocations(
        use:                 V,
        updatedEPS:          EPS[Entity, PropertyType],
        oldEOptP:            EOptionP[Entity, PropertyType],
        additionalTypes:     Set[ReferenceType],
        handleNewAllocation: (ReferenceType, ContextType, Int) ⇒ Unit
    ): Unit = {
        throw new UnsupportedOperationException
    }

    @inline protected[this] def continuationForAllocations(
        field:               Field,
        updatedEPS:          EPS[Entity, Property],
        oldEOptP:            EOptionP[Entity, Property],
        handleNewAllocation: (ReferenceType, ContextType, Int) ⇒ Unit
    )(
        implicit
        @nowarn state: TypeProviderState
    ): Unit = {
        throw new UnsupportedOperationException
    }

    private[cg] def isPossibleType(use: V, tpe: ReferenceType): Boolean = {
        val rv = use.value.asReferenceValue
        val lut = rv.leastUpperType
        if (lut.isDefined && !project.classHierarchy.isSubtypeOf(tpe, lut.get))
            false
        else
            rv.allValues.exists {
                case sv: IsSReferenceValue[_] ⇒
                    val tub = sv.theUpperTypeBound
                    if (sv.isPrecise) {
                        tpe eq tub
                    } else {
                        project.classHierarchy.isSubtypeOf(tpe, tub) &&
                            // Exclude unknown types even if the upper bound is Object for
                            // consistency with CHA and bounds != Object
                            ((tub ne ObjectType.Object) || tpe.isArrayType ||
                                project.classFile(tpe.asObjectType).isDefined)
                    }

                case mv: IsMObjectValue ⇒
                    val typeBounds = mv.upperTypeBound
                    typeBounds.forall { supertype ⇒
                        project.classHierarchy.isSubtypeOf(tpe, supertype)
                    }

                case _: IsNullValue ⇒
                    false
            }
    }

    private[cg] def isPossibleType(field: Field, tpe: ReferenceType): Boolean = {
        project.classHierarchy.isSubtypeOf(tpe, field.fieldType.asReferenceType)
    }
}

/**
 * Provides types based only on local, static type information. Never registers any dependencies,
 * the continuation function throws an error if called anyway.
 */
class CHATypeProvider(val project: SomeProject) extends TypeProvider {

    override type ContextType = SimpleContext
    override type InformationType = Null
    override type PropertyType = Nothing

    val usedPropertyKinds: Set[PropertyBounds] = Set.empty

    @inline override def typesProperty(
        use: V, context: SimpleContext, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeProviderState): Null = null

    @inline override def typesProperty(
        field: Field, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeProviderState
    ): Null = null

    def foreachType(
        use: V, typesProperty: Null, additionalTypes: Set[ReferenceType]
    )(handleType: ReferenceType ⇒ Unit): Unit = {
        additionalTypes.foreach(handleType)
        val rvs = use.value.asReferenceValue.allValues
        for (rv ← rvs) rv match {
            case sv: IsSReferenceValue[_] ⇒
                if (sv.isPrecise) {
                    handleType(sv.theUpperTypeBound)
                } else {
                    if (sv.theUpperTypeBound.isObjectType) {
                        project.classHierarchy.allSubtypesForeachIterator(
                            sv.theUpperTypeBound.asObjectType, reflexive = true
                        ).filter { subtype ⇒
                                val cfOption = project.classFile(subtype)
                                cfOption.isDefined && {
                                    val cf = cfOption.get
                                    !cf.isInterfaceDeclaration && !cf.isAbstract
                                }
                            }.foreach(handleType)
                    } else handleType(sv.theUpperTypeBound)
                }

            case mv: IsMObjectValue ⇒
                val typeBounds = mv.upperTypeBound
                val remainingTypeBounds = typeBounds.tail
                val firstTypeBound = typeBounds.head
                val potentialTypes = project.classHierarchy.allSubtypesForeachIterator(
                    firstTypeBound, reflexive = true
                ).filter { subtype ⇒
                    val cfOption = project.classFile(subtype)
                    cfOption.isDefined && {
                        val cf = cfOption.get
                        !cf.isInterfaceDeclaration && !cf.isAbstract &&
                            remainingTypeBounds.forall { supertype ⇒
                                project.classHierarchy.isSubtypeOf(subtype, supertype)
                            }
                    }
                }

                potentialTypes.foreach(handleType)

            case _: IsNullValue ⇒
            // TODO handle Null values?
        }
    }

    def foreachType(
        field: Field, typesProperty: Null
    )(handleType: ReferenceType ⇒ Unit): Unit = {
        if (field.fieldType.isObjectType) {
            project.classHierarchy.allSubtypesForeachIterator(
                field.fieldType.asObjectType, reflexive = true
            ).filter { subtype ⇒
                    val cfOption = project.classFile(subtype)
                    cfOption.isDefined && {
                        val cf = cfOption.get
                        !cf.isInterfaceDeclaration && !cf.isAbstract
                    }
                }.foreach(handleType)
        } else
            handleType(field.fieldType.asReferenceType)
    }

    @inline protected[this] override def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, Nothing],
        oldEOptP:        EOptionP[Entity, Nothing],
        additionalTypes: Set[ReferenceType],
        handleNewType:   ReferenceType ⇒ Unit
    ): Unit = {
        throw new UnsupportedOperationException
    }

    @inline protected[this] override def continuation(
        field:         Field,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        throw new UnsupportedOperationException
    }
}

/**
 * Fast type provider based on a global set of instantiated types.
 */
class RTATypeProvider(val project: SomeProject) extends TypeProvider {

    override type ContextType = SimpleContext
    override type InformationType = InstantiatedTypes
    override type PropertyType = InstantiatedTypes

    val usedPropertyKinds: Set[PropertyBounds] = Set(PropertyBounds.ub(InstantiatedTypes))

    private[this] val propertyStore = project.get(PropertyStoreKey)

    @inline override def typesProperty(
        use: V, context: SimpleContext, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeProviderState): InstantiatedTypes =
        typesProperty(depender, requiresDependency = true)

    @inline override def typesProperty(
        field: Field, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeProviderState
    ): InstantiatedTypes =
        typesProperty(depender, field.fieldType.isObjectType)

    @inline def typesProperty(
        depender: Entity, requiresDependency: Boolean
    )(implicit state: TypeProviderState): InstantiatedTypes = {
        val epk = EPK(project, InstantiatedTypes.key)
        val instantiatedTypesProperty = if (state.hasDependee(epk)) state.getProperty(epk)
        else propertyStore(epk)

        if (instantiatedTypesProperty.isRefinable && requiresDependency)
            state.addDependency(depender, instantiatedTypesProperty)

        if (instantiatedTypesProperty.hasUBP) instantiatedTypesProperty.ub
        else NoInstantiatedTypes
    }

    @inline override def foreachType(
        use: V, typesProperty: InstantiatedTypes, additionalTypes: Set[ReferenceType]
    )(
        handleType: ReferenceType ⇒ Unit
    ): Unit = {
        // The InstantiatedTypes do not track array types, we just assume them to be instantiated
        use.value.asReferenceValue.allValues.foreach {
            case av: IsSArrayValue ⇒
                handleType(av.theUpperTypeBound)
            case _ ⇒
        }
        typesProperty.types.iterator.filter { tpe ⇒
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleType)
    }

    @inline override def foreachType(
        field: Field, typesProperty: InstantiatedTypes
    )(
        handleType: ReferenceType ⇒ Unit
    ): Unit = {
        // The InstantiatedTypes do not track array types, we just assume them to be instantiated
        if (field.fieldType.isArrayType)
            handleType(field.fieldType.asReferenceType)
        else
            typesProperty.types.iterator.filter(isPossibleType(field, _)).foreach(handleType)
    }

    @inline protected[this] def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, InstantiatedTypes],
        oldEOptP:        EOptionP[Entity, InstantiatedTypes],
        additionalTypes: Set[ReferenceType],
        handleNewType:   ReferenceType ⇒ Unit
    ): Unit = {
        val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0
        updatedEPS.ub.dropOldest(seenTypes).filter { tpe ⇒
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleNewType)
    }

    @inline protected[this] def continuation(
        field:         Field,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        val seenTypes =
            if (oldEOptP.hasUBP) oldEOptP.ub.asInstanceOf[InstantiatedTypes].numElements else 0
        updatedEPS.ub.asInstanceOf[InstantiatedTypes].dropOldest(seenTypes).filter {
            isPossibleType(field, _)
        }.foreach(handleNewType)
    }
}

/**
 * Configurable type provider for the XTA family of call graphs. Based on the given
 * [[TypeSetEntitySelector]], XTA, MTA, FTA or CTA behavior can be produced. Types are stored per
 * entity plus a global set of types.
 */
class PropagationBasedTypeProvider(
        val project:           SomeProject,
        typeSetEntitySelector: TypeSetEntitySelector
) extends TypeProvider {

    override type ContextType = SimpleContext
    override type InformationType = (InstantiatedTypes, InstantiatedTypes)
    override type PropertyType = InstantiatedTypes

    val usedPropertyKinds: Set[PropertyBounds] = Set(PropertyBounds.ub(InstantiatedTypes))

    private[this] val propertyStore = project.get(PropertyStoreKey)

    @inline override def typesProperty(
        use: V, context: SimpleContext, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeProviderState): (InstantiatedTypes, InstantiatedTypes) = {
        (
            getProperty(typeSetEntitySelector(context.method), depender, requiresDependency = true),
            getProperty(project, depender, requiresDependency = true)
        )
    }

    @inline override def typesProperty(
        field: Field, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeProviderState
    ): (InstantiatedTypes, InstantiatedTypes) = {
        (
            getProperty(
                typeSetEntitySelector(field), depender, field.fieldType.isObjectType
            ),
                getProperty(project, depender, field.fieldType.isObjectType)
        )
    }

    @inline private[this] def getProperty(
        entity: TypeSetEntity, depender: Entity, requiresDependency: Boolean
    )(implicit state: TypeProviderState): InstantiatedTypes = {
        val epk = EPK(entity, InstantiatedTypes.key)
        val instantiatedTypesProperty = if (state.hasDependee(epk)) state.getProperty(epk)
        else propertyStore(epk)

        if (instantiatedTypesProperty.isRefinable && requiresDependency)
            state.addDependency(depender, instantiatedTypesProperty)

        if (instantiatedTypesProperty.hasUBP) instantiatedTypesProperty.ub
        else NoInstantiatedTypes
    }

    @inline override def foreachType(
        use:             V,
        typesProperty:   (InstantiatedTypes, InstantiatedTypes),
        additionalTypes: Set[ReferenceType]
    )(
        handleType: ReferenceType ⇒ Unit
    ): Unit = {
        typesProperty._1.types.iterator.filter { tpe ⇒
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleType)
        typesProperty._2.types.iterator.filter { tpe ⇒
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleType)
    }

    @inline override def foreachType(
        field:         Field,
        typesProperty: (InstantiatedTypes, InstantiatedTypes)
    )(
        handleType: ReferenceType ⇒ Unit
    ): Unit = {
        typesProperty._1.types.iterator.filter(isPossibleType(field, _)).foreach(handleType)
        typesProperty._2.types.iterator.filter(isPossibleType(field, _)).foreach(handleType)
    }

    @inline protected[this] def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, InstantiatedTypes],
        oldEOptP:        EOptionP[Entity, InstantiatedTypes],
        additionalTypes: Set[ReferenceType],
        handleNewType:   ReferenceType ⇒ Unit
    ): Unit = {
        val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0
        updatedEPS.ub.dropOldest(seenTypes).filter { tpe ⇒
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleNewType)
    }

    @inline protected[this] def continuation(
        field:         Field,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        val seenTypes =
            if (oldEOptP.hasUBP) oldEOptP.ub.asInstanceOf[InstantiatedTypes].numElements else 0
        updatedEPS.ub.asInstanceOf[InstantiatedTypes].dropOldest(seenTypes).filter {
            isPossibleType(field, _)
        }.foreach(handleNewType)
    }
}

/**
 * Functionality for providing types based on individual points-to information, e.g., CFA.
 * Points-to information is stored per variable.
 */
trait PointsToTypeProvider[ElementType, PointsToSet >: Null <: PointsToSetLike[ElementType, _, PointsToSet]]
    extends TypeProvider {

    override type InformationType = PointsToSet
    override type PropertyType = PointsToSet

    protected[this] val pointsToProperty: PropertyKey[PointsToSet]
    protected[this] def emptyPointsToSet: PointsToSet

    private[this] val propertyStore = project.get(PropertyStoreKey)
    protected[this] implicit val formalParameters: VirtualFormalParameters =
        project.get(VirtualFormalParametersKey)
    protected[this] implicit val definitionSites: DefinitionSites = project.get(DefinitionSitesKey)
    protected[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    protected[this] def createPointsToSet(
        pc:            Int,
        context:       ContextType,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean       = false
    ): PointsToSet

    val usedPropertyKinds: Set[PropertyBounds] = PropertyBounds.ubs(pointsToProperty)

    def typesProperty(
        use: V, context: ContextType, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeProviderState): PointsToSet = {
        use.definedBy.foldLeft(emptyPointsToSet) { (result, defSite) ⇒
            val pc = pcOfDefSite(defSite)(stmts)
            if (ai.isImmediateVMException(pc)) {
                // FIXME -  we need to get the actual exception type here
                combine(
                    result,
                    createPointsToSet(
                        ai.pcOfImmediateVMException(pc),
                        context,
                        ObjectType.Throwable,
                        isConstant = false
                    )
                )
            } else {
                combine(result, currentPointsTo(depender, toEntity(pc, context)))
            }
        }
    }

    override def typesProperty(
        field: Field, fieldAllocation: DefinitionSite, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeProviderState
    ): PointsToSet = {
        val objects = currentPointsTo(
            depender,
            toEntity(fieldAllocation.pc, new SimpleContext(declaredMethods(fieldAllocation.method)))
        )
        var pointsTo = emptyPointsToSet
        objects.forNewestNElements(objects.numElements) { as ⇒
            pointsTo = combine(pointsTo, currentPointsTo(depender, (as, field)))
        }
        pointsTo
    }

    @inline protected[this] def combine(pts1: PointsToSet, pts2: PointsToSet): PointsToSet

    @inline override def foreachType(
        use: V, typesProperty: PointsToSet, additionalTypes: Set[ReferenceType]
    )(
        handleType: ReferenceType ⇒ Unit
    ): Unit = {
        typesProperty.forNewestNTypes(typesProperty.numTypes) { tpe ⇒
            if (isPossibleType(use, tpe) || additionalTypes.contains(tpe)) handleType(tpe)
        }
    }

    @inline override def foreachType(
        field: Field, typesProperty: PointsToSet
    )(
        handleType: ReferenceType ⇒ Unit
    ): Unit = {
        typesProperty.forNewestNTypes(typesProperty.numTypes) { tpe ⇒
            if (isPossibleType(field, tpe)) handleType(tpe)
        }
    }

    @inline protected[this] def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, PointsToSet],
        oldEOptP:        EOptionP[Entity, PointsToSet],
        additionalTypes: Set[ReferenceType],
        handleNewType:   ReferenceType ⇒ Unit
    ): Unit = {
        val ub = updatedEPS.ub
        val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numTypes else 0
        ub.forNewestNTypes(ub.numTypes - seenTypes) { tpe ⇒
            if (isPossibleType(use, tpe) || additionalTypes.contains(tpe)) handleNewType(tpe)
        }
    }

    @inline protected[this] def continuation(
        field:         Field,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        val ub = updatedEPS.ub.asInstanceOf[PointsToSet]
        val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.asInstanceOf[PointsToSet].numTypes else 0
        ub.forNewestNTypes(ub.numTypes - seenTypes) { tpe ⇒
            if (isPossibleType(field, tpe)) handleNewType(tpe)
        }
    }

    @inline protected[this] def currentPointsTo(
        depender: Entity,
        dependee: Entity
    )(implicit state: TypeProviderState): PointsToSet = {
        val epk = EPK(dependee, pointsToProperty)
        val p2s = if (state.hasDependee(epk)) state.getProperty(epk) else propertyStore(epk)

        if (p2s.isRefinable)
            state.addDependency(depender, p2s)

        pointsToUB(p2s)
    }

    @inline private[this] def pointsToUB(eOptP: EOptionP[Entity, PointsToSet]): PointsToSet = {
        if (eOptP.hasUBP)
            eOptP.ub
        else
            emptyPointsToSet
    }

    /**
     * Given a definition site (value origin) in a certain method, this returns the
     * entity to be used to attach/retrieve points-to information from.
     */
    @inline protected[this] def toEntity(pc: Int, context: Context)(
        implicit
        formalParameters: VirtualFormalParameters, definitionSites: DefinitionSites
    ): Entity = {
        if (ai.isMethodExternalExceptionOrigin(pc)) {
            val actualPC = ai.pcOfMethodExternalException(pc)
            CallExceptions(toEntity(actualPC, context).asInstanceOf[DefinitionSite])
        } else if (ai.isImmediateVMException(pc)) {
            val actualPC = ai.pcOfImmediateVMException(pc)
            definitionSites(context.method.definedMethod, actualPC)
        } else if (pc < 0) {
            formalParameters.apply(context.method)(-1 - pc)
        } else {
            definitionSites(context.method.definedMethod, pc)
        }
    }
}

/**
 * Context-insensitive points-to type provider for the 0-CFA algorithm.
 */
class TypesPointsToTypeProvider(val project: SomeProject)
    extends PointsToTypeProvider[ReferenceType, TypeBasedPointsToSet] {

    override type ContextType = SimpleContext

    protected[this] val pointsToProperty: PropertyKey[TypeBasedPointsToSet] =
        TypeBasedPointsToSet.key

    protected[this] val emptyPointsToSet: TypeBasedPointsToSet = NoTypes

    override def typesProperty(
        field: Field, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeProviderState
    ): TypeBasedPointsToSet = {
        val types = project.classHierarchy.allSubtypes(field.classFile.thisType, reflexive = true)
        types.foldLeft(emptyPointsToSet) { (result, tpe) ⇒
            combine(result, currentPointsTo(depender, (tpe, field)))
        }
    }

    @inline override protected[this] def createPointsToSet(
        pc:            Int,
        context:       SimpleContext,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean       = false
    ): TypeBasedPointsToSet = TypeBasedPointsToSet(UIDSet(allocatedType))

    @inline protected[this] def combine(
        pts1: TypeBasedPointsToSet,
        pts2: TypeBasedPointsToSet
    ): TypeBasedPointsToSet = {
        pts1.included(pts2)
    }
}

/**
 * Type provider with 1-call sensitivity for objects, for the 0-1-CFA algorithm.
 */
class AllocationSitesPointsToTypeProvider(val project: SomeProject)
    extends PointsToTypeProvider[AllocationSite, AllocationSitePointsToSet] {

    val mergeStringBuilderBuffer: Boolean =
        project.config.getBoolean(mergeStringBuilderBufferConfigKey)
    val mergeStringConstants: Boolean = project.config.getBoolean(mergeStringConstsConfigKey)
    val mergeClassConstants: Boolean = project.config.getBoolean(mergeClassConstsConfigKey)
    val mergeExceptions: Boolean = project.config.getBoolean(mergeExceptionsConfigKey)

    private var exceptionPointsToSets: IntMap[AllocationSitePointsToSet] = IntMap()

    private[this] val fieldAccesses: FieldAccessInformation = project.get(FieldAccessInformationKey)

    override type ContextType = Context

    override val providesAllocations: Boolean = true

    override def typesProperty(
        field: Field, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeProviderState
    ): AllocationSitePointsToSet = {
        fieldAccesses.writeAccesses(field).foldLeft(emptyPointsToSet) { (result, access) ⇒
            val eOptP = propertyStore(access._1, TACAI.key)
            eOptP match {
                case UBPS(tac: TheTACAI, isFinal) ⇒
                    if (!isFinal)
                        state.addDependency((depender, access._1, access._2), eOptP)

                    val theTAC = tac.theTAC
                    access._2.foldLeft(result) { (result, pc) ⇒
                        theTAC.stmts(theTAC.properStmtIndexForPC(pc)).asPutField.objRef.asVar.definedBy.foldLeft(result) { (result, defSite) ⇒
                            val defPC = if (defSite < 0) defSite else theTAC.stmts(defSite).pc
                            combine(
                                result,
                                typesProperty(field, DefinitionSite(access._1, defPC), depender)
                            )
                        }
                    }

                case _ ⇒
                    state.addDependency((depender, access._1, access._2), eOptP)
                    result
            }
        }
    }

    @inline override def foreachAllocation(
        use: V, typesProperty: AllocationSitePointsToSet, additionalTypes: Set[ReferenceType]
    )(
        handleAllocation: (ReferenceType, Context, Int) ⇒ Unit
    ): Unit = {
        typesProperty.forNewestNElements(typesProperty.numElements) { as ⇒
            val (method, pc, typeId) = longToAllocationSite(as)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(use, tpe) || additionalTypes.contains(tpe))
                handleAllocation(
                    tpe,
                    method.map(new SimpleContext(_)).getOrElse(NoContext),
                    pc
                )
        }
    }

    @inline override def foreachAllocation(
        field: Field, typesProperty: AllocationSitePointsToSet
    )(
        handleAllocation: (ReferenceType, Context, Int) ⇒ Unit
    ): Unit = {
        typesProperty.forNewestNElements(typesProperty.numElements) { as ⇒
            val (method, pc, typeId) = longToAllocationSite(as)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(field, tpe))
                handleAllocation(
                    tpe,
                    method.map(new SimpleContext(_)).getOrElse(NoContext),
                    pc
                )
        }
    }

    @inline protected[this] override def continuation(
        field:         Field,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {

        updatedEPS.ub match {
            case _: AllocationSitePointsToSet ⇒
                super.continuation(field, updatedEPS, oldEOptP, handleNewType)
            case tac: TheTACAI ⇒
                val theTAC = tac.theTAC
                state.dependersOf(updatedEPS.toEPK).foreach {
                    case (depender: Entity, method: Method, pcs: PCs) ⇒
                        pcs.foreach { pc ⇒
                            val putField = theTAC.stmts(theTAC.properStmtIndexForPC(pc)).asPutField
                            putField.objRef.asVar.definedBy.foreach { defSite ⇒
                                val defPC = if (defSite < 0) defSite else theTAC.stmts(defSite).pc
                                val objects = currentPointsTo(
                                    depender,
                                    toEntity(defPC, new SimpleContext(declaredMethods(method)))
                                )

                                objects.forNewestNElements(objects.numElements) { as ⇒
                                    val pts = currentPointsTo(depender, (as, field))
                                    pts.forNewestNTypes(pts.numTypes)(handleNewType)
                                }
                            }
                        }
                }
        }
    }

    @inline protected[this] override def continuationForAllocations(
        use:                 V,
        updatedEPS:          EPS[Entity, PropertyType],
        oldEOptP:            EOptionP[Entity, PropertyType],
        additionalTypes:     Set[ReferenceType],
        handleNewAllocation: (ReferenceType, Context, Int) ⇒ Unit
    ): Unit = {
        val ub = updatedEPS.ub
        val seenElements = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0
        ub.forNewestNElements(ub.numElements - seenElements) { as ⇒
            val (method, pc, typeId) = longToAllocationSite(as)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(use, tpe) || additionalTypes.contains(tpe))
                handleNewAllocation(
                    tpe,
                    method.map(new SimpleContext(_)).getOrElse(NoContext),
                    pc
                )
        }
    }

    @inline protected[this] override def continuationForAllocations(
        field:               Field,
        updatedEPS:          EPS[Entity, Property],
        oldEOptP:            EOptionP[Entity, Property],
        handleNewAllocation: (ReferenceType, Context, Int) ⇒ Unit
    )(implicit state: TypeProviderState): Unit = {
        def handleAllocation(as: AllocationSite): Unit = {
            val (method, pc, typeId) = longToAllocationSite(as)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(field, tpe))
                handleNewAllocation(
                    tpe,
                    method.map(new SimpleContext(_)).getOrElse(NoContext),
                    pc
                )
        }

        val ub = updatedEPS.ub
        ub match {
            case pts: AllocationSitePointsToSet ⇒
                val seenElements = if (oldEOptP.hasUBP)
                    oldEOptP.ub.asInstanceOf[AllocationSitePointsToSet].numElements
                else
                    0
                updatedEPS.e match {
                    case (_, `field`) ⇒
                        pts.forNewestNElements(pts.numElements - seenElements)(handleAllocation)
                    case _ ⇒
                        pts.forNewestNElements(pts.numElements - seenElements) { oas ⇒
                            state.dependersOf(updatedEPS.toEPK).foreach { depender ⇒
                                val objects = currentPointsTo(depender, (oas, field))
                                objects.forNewestNElements(objects.numElements)(handleAllocation)
                            }
                        }
                }
            case tac: TheTACAI ⇒
                val theTAC = tac.theTAC
                state.dependersOf(updatedEPS.toEPK).foreach {
                    case (depender: Entity, method: Method, pcs: PCs) ⇒
                        pcs.foreach { pc ⇒
                            val putField = theTAC.stmts(theTAC.properStmtIndexForPC(pc)).asPutField
                            putField.objRef.asVar.definedBy.foreach { defSite ⇒
                                val defPC = if (defSite < 0) defSite else theTAC.stmts(defSite).pc
                                val objects = currentPointsTo(
                                    depender,
                                    toEntity(defPC, new SimpleContext(declaredMethods(method)))
                                )

                                objects.forNewestNElements(objects.numElements) { as ⇒
                                    val pts = currentPointsTo(depender, (as, field))
                                    pts.forNewestNElements(pts.numElements)(handleAllocation)
                                }
                            }
                        }
                }
        }
    }

    protected[this] val pointsToProperty: PropertyKey[AllocationSitePointsToSet] =
        AllocationSitePointsToSet.key

    protected[this] val emptyPointsToSet: AllocationSitePointsToSet = NoAllocationSites

    @inline protected[this] def createPointsToSet(
        pc:            Int,
        context:       Context,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean       = false
    ): AllocationSitePointsToSet = {
        @inline def createNewPointsToSet(): AllocationSitePointsToSet = {
            val as = allocationSiteToLong(context.method, pc, allocatedType, isEmptyArray)
            AllocationSitePointsToSet1(as, allocatedType)
        }

        (allocatedType.id: @switch) match {
            case StringBuilderId ⇒
                if (mergeStringBuilderBuffer)
                    stringBuilderPointsToSet
                else
                    createNewPointsToSet()
            case StringBufferId ⇒
                if (mergeStringBuilderBuffer)
                    stringBufferPointsToSet
                else
                    createNewPointsToSet()
            case StringId ⇒
                if (mergeStringConstants && isConstant)
                    stringConstPointsToSet
                else
                    createNewPointsToSet()
            case ClassId ⇒
                if (mergeClassConstants && isConstant)
                    classConstPointsToSet
                else
                    createNewPointsToSet()
            case _ ⇒
                if (mergeExceptions &&
                    project.classHierarchy.isSubtypeOf(allocatedType, ObjectType.Throwable)) {
                    val ptsO = exceptionPointsToSets.get(allocatedType.id)
                    if (ptsO.isDefined)
                        ptsO.get
                    else {
                        val newPts = mergedPointsToSetForType(allocatedType)
                        exceptionPointsToSets += allocatedType.id → newPts
                        newPts
                    }
                } else
                    createNewPointsToSet()
        }
    }

    @inline protected[this] def combine(
        pts1: AllocationSitePointsToSet,
        pts2: AllocationSitePointsToSet
    ): AllocationSitePointsToSet = {
        pts1.included(pts2)
    }
}