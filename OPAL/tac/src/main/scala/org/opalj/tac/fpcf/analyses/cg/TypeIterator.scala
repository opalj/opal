/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.annotation.nowarn
import scala.annotation.switch

import scala.collection.immutable.IntMap

import org.opalj.br.DeclaredField
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.ObjectType.ClassId
import org.opalj.br.ObjectType.StringBufferId
import org.opalj.br.ObjectType.StringBuilderId
import org.opalj.br.ObjectType.StringId
import org.opalj.br.PCs
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.CallStringContextProvider
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.analyses.SimpleContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.fpcf.properties.SimpleContext
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.NoInstantiatedTypes
import org.opalj.br.fpcf.properties.fieldaccess.AccessReceiver
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet1
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSites
import org.opalj.br.fpcf.properties.pointsto.NoTypes
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.fpcf.properties.pointsto.allocationSiteLongToTypeId
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong
import org.opalj.br.fpcf.properties.pointsto.longToAllocationSite
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.fpcf.UBPS
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
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.value.IsMObjectValue
import org.opalj.value.IsNullValue
import org.opalj.value.IsSArrayValue
import org.opalj.value.IsSReferenceValue
import org.opalj.value.ValueInformation

/**
 * Core class of the call-graph framework: Provides type and (if available) points-to information to
 * client classes. Each type iterator represents one traditional call-graph algorithm.
 *
 * Type iterators are responsible for managing the dependencies for their internal information
 * themselves. They provide suitable continuation functions to be invoked from an analysis'
 * continuation in order to process these opaque dependencies.
 *
 * @author Dominik Helm
 */
abstract class TypeIterator(val project: SomeProject) extends ContextProvider {

    protected[cg] type InformationType
    protected[cg] type PropertyType <: Property

    val usedPropertyKinds: Set[PropertyBounds]

    def typesProperty(
        use: V, context: ContextType, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeIteratorState): InformationType

    def typesProperty(
        field: DeclaredField, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): InformationType

    def typesProperty(
        field:           DeclaredField,
        fieldAllocation: DefinitionSite,
        depender:        Entity,
        context:         Context,
        stmts:           Array[Stmt[V]]
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): InformationType = {
        typesProperty(field, depender)
    }

    def foreachType(
        use:             V,
        typesProperty:   InformationType,
        additionalTypes: Set[ReferenceType] = Set.empty
    )(handleType: ReferenceType => Unit): Unit

    def foreachType(
        field:         DeclaredField,
        typesProperty: InformationType
    )(handleType: ReferenceType => Unit): Unit

    def foreachAllocation(
        use:             V,
        context:         Context,
        stmts:           Array[Stmt[V]],
        typesProperty:   InformationType,
        additionalTypes: Set[ReferenceType] = Set.empty
    )(
        handleAllocation: (ReferenceType, Context, Int) => Unit
    ): Unit = {
        var hasUnknownAllocation = false
        use.definedBy.foreach { index =>
            if (index >= 0) {
                val allocO = stmts(index) match {
                    case Assignment(pc, _, New(_, tpe))         => Some((tpe, pc))
                    case Assignment(pc, _, NewArray(_, _, tpe)) => Some((tpe, pc))
                    case Assignment(pc, _, c: Const)            => Some((c.tpe.asObjectType, pc))
                    case Assignment(pc, _, fc: FunctionCall[V]) => Some((fc.descriptor.returnType.asReferenceType, pc))
                    case _ =>
                        hasUnknownAllocation = true
                        None
                }
                if (allocO.isDefined)
                    handleAllocation(
                        allocO.get._1,
                        context,
                        allocO.get._2
                    )
            } else {
                hasUnknownAllocation = true
            }
        }
        if (hasUnknownAllocation) {
            // IMPROVE: Could use the more precise type information here instead of just the
            // least upper type
            handleAllocation(
                use.value.asReferenceValue.leastUpperType.getOrElse(ObjectType.Object),
                NoContext,
                -1
            )
        }
    }

    def foreachAllocation(
        field: DeclaredField, typesProperty: InformationType
    )(
        handleAllocation: (ReferenceType, Context, Int) => Unit
    ): Unit = {
        handleAllocation(field.fieldType.asReferenceType, NoContext, -1)
    }

    def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, PropertyType],
        additionalTypes: Set[ReferenceType]        = Set.empty
    )(
        handleNewType: ReferenceType => Unit
    )(implicit state: TypeIteratorState): Unit = {
        val epk = updatedEPS.toEPK
        val oldEOptP = state.getProperty(epk)

        continuation(use, updatedEPS, oldEOptP, additionalTypes, handleNewType)
    }

    def continuation(
        field:      DeclaredField,
        updatedEPS: EPS[Entity, Property]
    )(
        handleNewType: ReferenceType => Unit
    )(implicit state: TypeIteratorState): Unit = {
        val epk = updatedEPS.toEPK
        val oldEOptP = state.getProperty(epk)

        continuation(field, updatedEPS, oldEOptP, handleNewType)
    }

    @inline protected[this] def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, PropertyType],
        oldEOptP:        EOptionP[Entity, PropertyType],
        additionalTypes: Set[ReferenceType],
        handleNewType:   ReferenceType => Unit
    ): Unit

    @inline protected[this] def continuation(
        field:         DeclaredField,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType => Unit
    )(implicit state: TypeIteratorState): Unit

    def continuationForAllocations(
        use:             V,
        updatedEPS:      EPS[Entity, PropertyType],
        additionalTypes: Set[ReferenceType]        = Set.empty
    )(
        handleNewAllocation: (ReferenceType, Context, Int) => Unit
    )(implicit state: TypeIteratorState): Unit = {
        val epk = updatedEPS.toEPK
        val oldEOptP = state.getProperty(epk)

        continuationForAllocations(use, updatedEPS, oldEOptP, additionalTypes, handleNewAllocation)
    }

    def continuationForAllocations(
        field:      DeclaredField,
        updatedEPS: EPS[Entity, Property]
    )(
        handleNewAllocation: (ReferenceType, Context, Int) => Unit
    )(implicit state: TypeIteratorState): Unit = {
        val epk = updatedEPS.toEPK
        val oldEOptP = state.getProperty(epk)

        continuationForAllocations(field, updatedEPS, oldEOptP, handleNewAllocation)
    }

    @inline protected[this] def continuationForAllocations(
        use:                 V,
        updatedEPS:          EPS[Entity, PropertyType],
        oldEOptP:            EOptionP[Entity, PropertyType],
        additionalTypes:     Set[ReferenceType],
        handleNewAllocation: (ReferenceType, Context, Int) => Unit
    ): Unit = {
        // Do nothing
    }

    @inline protected[this] def continuationForAllocations(
        field:               DeclaredField,
        updatedEPS:          EPS[Entity, Property],
        oldEOptP:            EOptionP[Entity, Property],
        handleNewAllocation: (ReferenceType, Context, Int) => Unit
    )(
        implicit
        @nowarn state: TypeIteratorState
    ): Unit = {
        // Do nothing
    }

    private[cg] def isPossibleType(use: V, tpe: ReferenceType): Boolean = {
        val ch = project.classHierarchy
        val rv = use.value.asReferenceValue
        val lut = rv.leastUpperType
        if (lut.isDefined && !ch.isSubtypeOf(tpe, lut.get))
            false
        else
            rv.allValues.exists {
                case sv: IsSReferenceValue[_] =>
                    val tub = sv.theUpperTypeBound
                    if (sv.isPrecise) {
                        tpe eq tub
                    } else {
                        ch.isSubtypeOf(tpe, tub) &&
                            // Exclude unknown types even if the upper bound is Object for
                            // consistency with CHA and bounds != Object
                            ((tub ne ObjectType.Object) || tpe.isArrayType ||
                                project.classFile(tpe.asObjectType).isDefined) &&
                                ch.isASubtypeOf(tpe, use.value.asReferenceValue.upperTypeBound).isNotNo
                    }

                case mv: IsMObjectValue =>
                    val typeBounds = mv.upperTypeBound
                    typeBounds.forall { supertype =>
                        ch.isSubtypeOf(tpe, supertype)
                    }

                case _: IsNullValue =>
                    false
            }
    }

    private[cg] def isPossibleType(field: DeclaredField, tpe: ReferenceType): Boolean = {
        project.classHierarchy.isSubtypeOf(tpe, field.fieldType.asReferenceType)
    }
}

/**
 * Provides types based only on local, static type information. Never registers any dependencies,
 * the continuation function throws an error if called anyway.
 */
class CHATypeIterator(project: SomeProject)
    extends TypeIterator(project) with SimpleContextProvider {

    override type InformationType = Null
    override type PropertyType = Nothing

    val usedPropertyKinds: Set[PropertyBounds] = Set.empty

    @inline override def typesProperty(
        use: V, context: SimpleContext, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeIteratorState): Null = null

    @inline override def typesProperty(
        field: DeclaredField, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): Null = null

    def foreachType(
        use: V, typesProperty: Null, additionalTypes: Set[ReferenceType]
    )(handleType: ReferenceType => Unit): Unit = {
        additionalTypes.foreach(handleType)
        val ch = project.classHierarchy
        val rvs = use.value.asReferenceValue.allValues
        for (rv <- rvs) rv match {
            case sv: IsSReferenceValue[_] =>
                if (sv.isPrecise) {
                    handleType(sv.theUpperTypeBound)
                } else {
                    if (sv.theUpperTypeBound.isObjectType) {
                        ch.allSubtypesForeachIterator(
                            sv.theUpperTypeBound.asObjectType, reflexive = true
                        ).filter { subtype =>
                                val cfOption = project.classFile(subtype)
                                cfOption.isDefined && {
                                    val cf = cfOption.get
                                    !cf.isInterfaceDeclaration && !cf.isAbstract
                                } && {
                                    ch.isASubtypeOf(subtype, use.value.asReferenceValue.upperTypeBound)
                                }.isNotNo
                            }.foreach(handleType)
                    } else handleType(ObjectType.Object)
                }

            case mv: IsMObjectValue =>
                val typeBounds = mv.upperTypeBound
                val remainingTypeBounds = typeBounds.tail
                val firstTypeBound = typeBounds.head
                val potentialTypes = ch.allSubtypesForeachIterator(
                    firstTypeBound, reflexive = true
                ).filter { subtype =>
                    val cfOption = project.classFile(subtype)
                    cfOption.isDefined && {
                        val cf = cfOption.get
                        !cf.isInterfaceDeclaration && !cf.isAbstract &&
                            remainingTypeBounds.forall { supertype =>
                                ch.isSubtypeOf(subtype, supertype)
                            }
                    }
                }

                potentialTypes.foreach(handleType)

            case _: IsNullValue =>
            // TODO handle Null values?
        }
    }

    def foreachType(
        field: DeclaredField, typesProperty: Null
    )(handleType: ReferenceType => Unit): Unit = {
        if (field.fieldType.isObjectType) {
            project.classHierarchy.allSubtypesForeachIterator(
                field.fieldType.asObjectType, reflexive = true
            ).filter { subtype =>
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
        handleNewType:   ReferenceType => Unit
    ): Unit = {
        throw new UnsupportedOperationException
    }

    @inline protected[this] override def continuation(
        field:         DeclaredField,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType => Unit
    )(implicit state: TypeIteratorState): Unit = {
        throw new UnsupportedOperationException
    }
}

/**
 * Fast type iterator based on a global set of instantiated types.
 */
class RTATypeIterator(project: SomeProject)
    extends TypeIterator(project) with SimpleContextProvider {

    override type InformationType = InstantiatedTypes
    override type PropertyType = InstantiatedTypes

    val usedPropertyKinds: Set[PropertyBounds] = Set(PropertyBounds.ub(InstantiatedTypes))

    private[this] lazy val propertyStore = project.get(PropertyStoreKey)

    @inline override def typesProperty(
        use: V, context: SimpleContext, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeIteratorState): InstantiatedTypes =
        typesProperty(depender, requiresDependency = true)

    @inline override def typesProperty(
        field: DeclaredField, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): InstantiatedTypes =
        typesProperty(depender, field.fieldType.isObjectType)

    @inline def typesProperty(
        depender: Entity, requiresDependency: Boolean
    )(implicit state: TypeIteratorState): InstantiatedTypes = {
        val epk = EPK(project, InstantiatedTypes.key)
        val instantiatedTypesProperty = if (state.hasDependee(epk)) state.getProperty(epk)
        else propertyStore(epk)

        // val types = possibleTypes(use)

        if (instantiatedTypesProperty.isRefinable && requiresDependency)
            state.addDependency(depender, instantiatedTypesProperty)

        if (instantiatedTypesProperty.hasUBP) instantiatedTypesProperty.ub
        else NoInstantiatedTypes
    }

    @inline override def foreachType(
        use: V, typesProperty: InstantiatedTypes, additionalTypes: Set[ReferenceType]
    )(
        handleType: ReferenceType => Unit
    ): Unit = {
        // The InstantiatedTypes do not track array types, we just assume them to be instantiated
        use.value.asReferenceValue.allValues.foreach {
            case av: IsSArrayValue =>
                handleType(av.theUpperTypeBound)
            case _ =>
        }
        typesProperty.types.iterator.filter { tpe =>
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleType)
    }

    @inline override def foreachType(
        field: DeclaredField, typesProperty: InstantiatedTypes
    )(
        handleType: ReferenceType => Unit
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
        handleNewType:   ReferenceType => Unit
    ): Unit = {
        val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0
        updatedEPS.ub.dropOldest(seenTypes).filter { tpe =>
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleNewType)
    }

    @inline protected[this] def continuation(
        field:         DeclaredField,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType => Unit
    )(implicit state: TypeIteratorState): Unit = {
        val seenTypes =
            if (oldEOptP.hasUBP) oldEOptP.ub.asInstanceOf[InstantiatedTypes].numElements else 0
        updatedEPS.ub.asInstanceOf[InstantiatedTypes].dropOldest(seenTypes).filter {
            isPossibleType(field, _)
        }.foreach(handleNewType)
    }
}

/**
 * Configurable type iterator for the XTA family of call graphs. Based on the given
 * [[TypeSetEntitySelector]], XTA, MTA, FTA or CTA behavior can be produced. Types are stored per
 * entity plus a global set of types.
 */
class PropagationBasedTypeIterator(
        project:               SomeProject,
        typeSetEntitySelector: TypeSetEntitySelector
) extends TypeIterator(project) with SimpleContextProvider {

    override type InformationType = (InstantiatedTypes, InstantiatedTypes)
    override type PropertyType = InstantiatedTypes

    val usedPropertyKinds: Set[PropertyBounds] = Set(PropertyBounds.ub(InstantiatedTypes))

    private[this] lazy val propertyStore = project.get(PropertyStoreKey)

    @inline override def typesProperty(
        use: V, context: SimpleContext, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeIteratorState): (InstantiatedTypes, InstantiatedTypes) = {
        (
            getProperty(typeSetEntitySelector(context.method), depender, requiresDependency = true),
            getProperty(project, depender, requiresDependency = true)
        )
    }

    @inline override def typesProperty(
        field: DeclaredField, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
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
    )(implicit state: TypeIteratorState): InstantiatedTypes = {
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
        handleType: ReferenceType => Unit
    ): Unit = {
        typesProperty._1.types.iterator.filter { tpe =>
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleType)
        typesProperty._2.types.iterator.filter { tpe =>
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleType)
    }

    @inline override def foreachType(
        field:         DeclaredField,
        typesProperty: (InstantiatedTypes, InstantiatedTypes)
    )(
        handleType: ReferenceType => Unit
    ): Unit = {
        typesProperty._1.types.iterator.filter(isPossibleType(field, _)).foreach(handleType)
        typesProperty._2.types.iterator.filter(isPossibleType(field, _)).foreach(handleType)
    }

    @inline protected[this] def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, InstantiatedTypes],
        oldEOptP:        EOptionP[Entity, InstantiatedTypes],
        additionalTypes: Set[ReferenceType],
        handleNewType:   ReferenceType => Unit
    ): Unit = {
        val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0
        updatedEPS.ub.dropOldest(seenTypes).filter { tpe =>
            isPossibleType(use, tpe) || additionalTypes.contains(tpe)
        }.foreach(handleNewType)
    }

    @inline protected[this] def continuation(
        field:         DeclaredField,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType => Unit
    )(implicit state: TypeIteratorState): Unit = {
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
trait PointsToTypeIterator[ElementType, PointsToSet >: Null <: PointsToSetLike[ElementType, _, PointsToSet]]
    extends TypeIterator {

    override type InformationType = PointsToSet
    override type PropertyType = PointsToSet

    protected[this] def pointsToProperty: PropertyMetaInformation
    protected[this] def emptyPointsToSet: PointsToSet

    private[this] lazy val propertyStore = project.get(PropertyStoreKey)
    protected[this] implicit lazy val formalParameters: VirtualFormalParameters =
        project.get(VirtualFormalParametersKey)
    protected[this] implicit lazy val definitionSites: DefinitionSites = project.get(DefinitionSitesKey)
    private[this] implicit val typeIterator: TypeIterator = this

    protected[this] def createPointsToSet(
        pc:            Int,
        context:       ContextType,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean       = false
    ): PointsToSet

    val usedPropertyKinds: Set[PropertyBounds] = PropertyBounds.ubs(pointsToProperty, FieldWriteAccessInformation)

    def typesProperty(
        use: V, context: ContextType, depender: Entity, stmts: Array[Stmt[V]]
    )(implicit state: TypeIteratorState): PointsToSet = {
        use.definedBy.foldLeft(emptyPointsToSet) { (result, defSite) =>
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
                combine(
                    result, currentPointsTo(depender, pointsto.toEntity(defSite, context, stmts))
                )
            }
        }
    }

    override def typesProperty(
        field:           DeclaredField,
        fieldAllocation: DefinitionSite,
        depender:        Entity,
        context:         Context,
        stmts:           Array[Stmt[V]]
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): PointsToSet = {
        val objects = currentPointsTo(
            depender,
            pointsto.toEntity(fieldAllocation.pc, context, stmts)
        )
        var pointsTo = emptyPointsToSet
        objects.forNewestNElements(objects.numElements) { as =>
            pointsTo = combine(pointsTo, currentPointsTo(depender, (as, field)))
        }
        pointsTo
    }

    @inline protected[this] def combine(pts1: PointsToSet, pts2: PointsToSet): PointsToSet = {
        pts1.included(pts2)
    }

    @inline override def foreachType(
        use: V, typesProperty: PointsToSet, additionalTypes: Set[ReferenceType]
    )(
        handleType: ReferenceType => Unit
    ): Unit = {
        typesProperty.forNewestNTypes(typesProperty.numTypes) { tpe =>
            if (isPossibleType(use, tpe) || additionalTypes.contains(tpe)) handleType(tpe)
        }
    }

    @inline override def foreachType(
        field: DeclaredField, typesProperty: PointsToSet
    )(
        handleType: ReferenceType => Unit
    ): Unit = {
        typesProperty.forNewestNTypes(typesProperty.numTypes) { tpe =>
            if (isPossibleType(field, tpe)) handleType(tpe)
        }
    }

    @inline protected[this] def continuation(
        use:             V,
        updatedEPS:      EPS[Entity, PointsToSet],
        oldEOptP:        EOptionP[Entity, PointsToSet],
        additionalTypes: Set[ReferenceType],
        handleNewType:   ReferenceType => Unit
    ): Unit = {
        val ub = updatedEPS.ub
        val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numTypes else 0
        ub.forNewestNTypes(ub.numTypes - seenTypes) { tpe =>
            if (isPossibleType(use, tpe) || additionalTypes.contains(tpe)) handleNewType(tpe)
        }
    }

    @inline protected[this] def continuation(
        field:         DeclaredField,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType => Unit
    )(implicit state: TypeIteratorState): Unit = {
        val ub = updatedEPS.ub.asInstanceOf[PointsToSet]
        val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.asInstanceOf[PointsToSet].numTypes else 0
        ub.forNewestNTypes(ub.numTypes - seenTypes) { tpe =>
            if (isPossibleType(field, tpe)) handleNewType(tpe)
        }
    }

    @inline protected[this] def currentPointsTo(
        depender: Entity,
        dependee: Entity
    )(
        implicit
        state: TypeIteratorState
    ): PointsToSet = {
        val epk = EPK(dependee, pointsToProperty.key).asInstanceOf[EPK[Entity, PointsToSet]]
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

    def extractPropertyUB[E <: Entity, P <: Property](
        epk:           EPK[E, P],
        addDependency: EOptionP[E, P] => Unit
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): Option[P] = {
        val ep = if (state.hasDependee(epk)) state.getProperty(epk) else propertyStore(epk)
        ep match {
            case UBPS(ub, isFinal) =>
                if (!isFinal) addDependency(ep)
                Some(ub)
            case _ =>
                addDependency(ep)
                None
        }
    }
}

/**
 * Context-insensitive points-to type iterator for the 0-CFA algorithm.
 */
trait TypesBasedPointsToTypeIterator
    extends PointsToTypeIterator[ReferenceType, TypeBasedPointsToSet] {

    protected[this] def pointsToProperty: PropertyMetaInformation = TypeBasedPointsToSet

    protected[this] val emptyPointsToSet: TypeBasedPointsToSet = NoTypes

    override def typesProperty(
        field: DeclaredField, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): TypeBasedPointsToSet = {
        val types = project.classHierarchy.allSubtypes(field.declaringClassType, reflexive = true)
        types.foldLeft(emptyPointsToSet) { (result, tpe) =>
            combine(result, currentPointsTo(depender, (tpe, field)))
        }
    }

    @inline override protected[this] def createPointsToSet(
        pc:            Int,
        context:       ContextType,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean       = false
    ): TypeBasedPointsToSet = TypeBasedPointsToSet(UIDSet(allocatedType))
}

abstract class AbstractAllocationSitesPointsToTypeIterator(project: SomeProject)
    extends TypeIterator(project) with PointsToTypeIterator[AllocationSite, AllocationSitePointsToSet] {

    val mergeStringBuilderBuffer: Boolean =
        project.config.getBoolean(mergeStringBuilderBufferConfigKey)
    val mergeStringConstants: Boolean = project.config.getBoolean(mergeStringConstsConfigKey)
    val mergeClassConstants: Boolean = project.config.getBoolean(mergeClassConstsConfigKey)
    val mergeExceptions: Boolean = project.config.getBoolean(mergeExceptionsConfigKey)

    private var exceptionPointsToSets: IntMap[AllocationSitePointsToSet] = IntMap()

    @inline override def foreachAllocation(
        use:             V,
        context:         Context,
        stmts:           Array[Stmt[V]],
        typesProperty:   AllocationSitePointsToSet,
        additionalTypes: Set[ReferenceType]
    )(
        handleAllocation: (ReferenceType, Context, Int) => Unit
    ): Unit = {
        typesProperty.forNewestNElements(typesProperty.numElements) { as =>
            val (context, pc, typeId) = longToAllocationSite(as)(this)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(use, tpe) || additionalTypes.contains(tpe))
                handleAllocation(tpe, context, pc)
        }
    }

    @inline protected[this] override def continuationForAllocations(
        use:                 V,
        updatedEPS:          EPS[Entity, PropertyType],
        oldEOptP:            EOptionP[Entity, PropertyType],
        additionalTypes:     Set[ReferenceType],
        handleNewAllocation: (ReferenceType, Context, Int) => Unit
    ): Unit = {
        val ub = updatedEPS.ub
        val seenElements = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0
        ub.forNewestNElements(ub.numElements - seenElements) { as =>
            val (context, pc, typeId) = longToAllocationSite(as)(this)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(use, tpe) || additionalTypes.contains(tpe))
                handleNewAllocation(tpe, context, pc)
        }
    }

    protected[this] def pointsToProperty: PropertyMetaInformation = AllocationSitePointsToSet

    protected[this] val emptyPointsToSet: AllocationSitePointsToSet = NoAllocationSites

    @inline protected[this] def createPointsToSet(
        pc:            Int,
        context:       ContextType,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean       = false
    ): AllocationSitePointsToSet = {
        @inline def createNewPointsToSet(): AllocationSitePointsToSet = {
            val as = allocationSiteToLong(context, pc, allocatedType, isEmptyArray)
            AllocationSitePointsToSet1(as, allocatedType)
        }

        (allocatedType.id: @switch) match {
            case StringBuilderId =>
                if (mergeStringBuilderBuffer)
                    stringBuilderPointsToSet
                else
                    createNewPointsToSet()
            case StringBufferId =>
                if (mergeStringBuilderBuffer)
                    stringBufferPointsToSet
                else
                    createNewPointsToSet()
            case StringId =>
                if (mergeStringConstants && isConstant)
                    stringConstPointsToSet
                else
                    createNewPointsToSet()
            case ClassId =>
                if (mergeClassConstants && isConstant)
                    classConstPointsToSet
                else
                    createNewPointsToSet()
            case _ =>
                if (mergeExceptions &&
                    project.classHierarchy.isSubtypeOf(allocatedType, ObjectType.Throwable)) {
                    val ptsO = exceptionPointsToSets.get(allocatedType.id)
                    if (ptsO.isDefined)
                        ptsO.get
                    else {
                        val newPts = mergedPointsToSetForType(allocatedType)
                        exceptionPointsToSets += allocatedType.id -> newPts
                        newPts
                    }
                } else
                    createNewPointsToSet()
        }
    }
}

/**
 * Type iterator with 1-call sensitivity for objects, for the 0-1-CFA algorithm.
 */
class AllocationSitesPointsToTypeIterator(project: SomeProject)
    extends AbstractAllocationSitesPointsToTypeIterator(project) with SimpleContextProvider {
    implicit lazy val propertyStore: PropertyStore = project.get(PropertyStoreKey)

    override def typesProperty(
        field: DeclaredField, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): AllocationSitePointsToSet = {
        if (field.isDefinedField && field.definedField.isStatic) {
            // IMPROVE: Handle static case also for VirtualDeclaredFields once static information is available
            currentPointsTo(depender, field)
        } else {
            var result = emptyPointsToSet
            for {
                // Extract FieldWriteAccessInformation
                fai <- extractPropertyUB(
                    EPK(field, FieldWriteAccessInformation.key), state.addDependency(depender, _)
                )
                (accessContextId, _, receiver, _) <- fai.accesses
                // Extract TAC
                definedMethod = contextFromId(accessContextId).method
                method = contextFromId(accessContextId).method.definedMethod
                tacEP <- extractPropertyUB(
                    EPK(method, TACAI.key), state.addDependency((depender, definedMethod, receiver), _)
                )
                theTAC <- tacEP.tac
                defSite <- uVarForDefSites(receiver.get, theTAC.pcToIndex).definedBy
            } {
                val defPC = if (defSite < 0) defSite else theTAC.stmts(defSite).pc

                result = combine(
                    result,
                    typesProperty(
                        field, DefinitionSite(method, defPC), depender, newContext(definedMethod), theTAC.stmts
                    )
                )
            }

            result
        }
    }

    @inline override def foreachAllocation(
        field: DeclaredField, typesProperty: AllocationSitePointsToSet
    )(
        handleAllocation: (ReferenceType, Context, Int) => Unit
    ): Unit = {
        typesProperty.forNewestNElements(typesProperty.numElements) { as =>
            val (context, pc, typeId) = longToAllocationSite(as)(this)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(field, tpe))
                handleAllocation(tpe, context, pc)
        }
    }

    @inline protected[this] def handleAllocationTacUpdate(
        depender:      Entity,
        definedMethod: DefinedMethod,
        theTAC:        TACode[TACMethodParameter, DUVar[ValueInformation]],
        receiver:      (ValueInformation, PCs)
    )(
        handleAllocation: AllocationSite => Unit
    )(implicit state: TypeIteratorState): Unit = {
        uVarForDefSites(receiver, theTAC.pcToIndex).definedBy.foreach { defSite =>
            val defPC = if (defSite < 0) defSite else theTAC.stmts(defSite).pc
            val objects = currentPointsTo(
                depender,
                pointsto.toEntity(defPC, newContext(definedMethod), theTAC.stmts)(formalParameters, definitionSites, this)
            )

            objects.forNewestNElements(objects.numElements)(handleAllocation)
        }
    }

    @inline protected[this] override def continuation(
        field:         DeclaredField,
        updatedEPS:    EPS[Entity, Property],
        oldEOptP:      EOptionP[Entity, Property],
        handleNewType: ReferenceType => Unit
    )(implicit state: TypeIteratorState): Unit = {
        def handleType(as: AllocationSite): Unit = {
            val typeId = allocationSiteLongToTypeId(as)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(field, tpe))
                handleNewType(tpe)
        }

        def handleAllocationSiteTypes(depender: Entity, as: AllocationSite): Unit = {
            val objects = currentPointsTo(depender, (as, field))
            objects.forNewestNTypes(objects.numTypes) { tpe =>
                if (isPossibleType(field, tpe))
                    handleNewType(tpe)
            }
        }

        val ub = updatedEPS.ub
        ub match {
            case pts: AllocationSitePointsToSet =>
                val seenElements = if (oldEOptP.hasUBP)
                    oldEOptP.ub.asInstanceOf[AllocationSitePointsToSet].numElements
                else
                    0
                updatedEPS.e match {
                    case (_, `field`) =>
                        pts.forNewestNElements(pts.numElements - seenElements)(handleType)
                    case _ =>
                        state.dependersOf(updatedEPS.toEPK).foreach { depender =>
                            pts.forNewestNElements(pts.numElements - seenElements)(handleAllocationSiteTypes(depender, _))
                        }
                }
            case tac: TheTACAI =>
                val theTAC = tac.theTAC
                state.dependersOf(updatedEPS.toEPK).foreach {
                    case (depender: Entity, definedMethod: DefinedMethod, receiver: AnyRef) if receiver.isInstanceOf[Option[_]] &&
                        receiver.asInstanceOf[Option[_]].isDefined =>
                        handleAllocationTacUpdate(
                            depender, definedMethod, theTAC,
                            receiver.asInstanceOf[AccessReceiver].get
                        )(handleAllocationSiteTypes(depender, _))
                }

            case fai: FieldWriteAccessInformation =>
                continuationForFieldAccesses(updatedEPS, oldEOptP, fai)(handleAllocationSiteTypes)
        }
    }

    @inline protected[this] override def continuationForAllocations(
        field:               DeclaredField,
        updatedEPS:          EPS[Entity, Property],
        oldEOptP:            EOptionP[Entity, Property],
        handleNewAllocation: (ReferenceType, Context, Int) => Unit
    )(implicit state: TypeIteratorState): Unit = {
        def handleAllocation(as: AllocationSite): Unit = {
            val (context, pc, typeId) = longToAllocationSite(as)(this)
            val tpe = ReferenceType.lookup(typeId)
            if (isPossibleType(field, tpe))
                handleNewAllocation(tpe, context, pc)
        }

        def handlePointsToOfAllocationSite(depender: Entity, as: AllocationSite): Unit = {
            val pts = currentPointsTo(depender, (as, field))
            pts.forNewestNElements(pts.numElements)(handleAllocation)
        }

        val ub = updatedEPS.ub
        ub match {
            case pts: AllocationSitePointsToSet =>
                val seenElements = if (oldEOptP.hasUBP)
                    oldEOptP.ub.asInstanceOf[AllocationSitePointsToSet].numElements
                else
                    0
                updatedEPS.e match {
                    case (_, `field`) =>
                        pts.forNewestNElements(pts.numElements - seenElements)(handleAllocation)
                    case _ =>
                        pts.forNewestNElements(pts.numElements - seenElements) { oas =>
                            state.dependersOf(updatedEPS.toEPK).foreach(handlePointsToOfAllocationSite(_, oas))
                        }
                }

            case tac: TheTACAI =>
                state.dependersOf(updatedEPS.toEPK).foreach {
                    case (depender: Entity, definedMethod: DefinedMethod, receiver: AnyRef) if receiver.isInstanceOf[Option[_]] &&
                        receiver.asInstanceOf[Option[_]].isDefined =>
                        handleAllocationTacUpdate(
                            depender, definedMethod, tac.theTAC,
                            receiver.asInstanceOf[AccessReceiver].get
                        )(handlePointsToOfAllocationSite(depender, _))
                }

            case fai: FieldWriteAccessInformation =>
                continuationForFieldAccesses(updatedEPS, oldEOptP, fai)(handlePointsToOfAllocationSite)
        }
    }

    @inline private def continuationForFieldAccesses(
        updatedEPS: EPS[Entity, Property],
        oldEOptP:   EOptionP[Entity, Property],
        fai:        FieldWriteAccessInformation
    )(
        handleAllocationSite: (Entity, AllocationSite) => Unit
    )(implicit state: TypeIteratorState): Unit = {
        val (seenDirectAccesses, seenIndirectAccesses) = oldEOptP.asInstanceOf[EOptionP[DeclaredField, FieldWriteAccessInformation]] match {
            case UBP(fai) => (fai.numDirectAccesses, fai.numIndirectAccesses)
            case _        => (0, 0)
        }

        state.dependersOf(updatedEPS.toEPK).foreach { depender =>
            fai.getNewestAccesses(
                fai.numDirectAccesses - seenDirectAccesses,
                fai.numIndirectAccesses - seenIndirectAccesses
            ) foreach { wa =>
                    val definedMethod = contextFromId(wa._1).method.asDefinedMethod

                    val receiverOpt = wa._3

                    val tacEPK = EPK(definedMethod.definedMethod, TACAI.key)
                    val tacEP = if (state.hasDependee(tacEPK)) state.getProperty(tacEPK) else propertyStore(definedMethod.definedMethod, TACAI.key)
                    if (tacEP.isRefinable) state.addDependency((depender, definedMethod, receiverOpt), tacEP)

                    if (tacEP.hasUBP && tacEP.ub.tac.isDefined && receiverOpt.isDefined)
                        handleAllocationTacUpdate(depender, definedMethod, tacEP.ub.tac.get, receiverOpt.get)(handleAllocationSite(depender, _))
                }
        }
    }
}

/**
 * Context-sensitive points-to type iterator for the k-l-CFA algorithm.
 */
class CFA_k_l_TypeIterator(project: SomeProject, val k: Int, val l: Int)
    extends AbstractAllocationSitesPointsToTypeIterator(project)
    with CallStringContextProvider {

    assert(k > 0 && l > 0 && k >= l - 1)

    override def typesProperty(
        field: DeclaredField, depender: Entity
    )(
        implicit
        propertyStore: PropertyStore,
        state:         TypeIteratorState
    ): AllocationSitePointsToSet = {
        if (field.isDefinedField && field.definedField.isStatic) {
            // IMPROVE: Handle static case also for VirtualDeclaredFields when static information is available on them
            currentPointsTo(depender, field)
        } else {
            var result = emptyPointsToSet;
            for {
                // Extract FieldWriteAccessInformation
                fai <- extractPropertyUB(
                    EPK(field, FieldWriteAccessInformation.key), state.addDependency(depender, _)
                )
                (accessContextId, _, receiver, _) <- fai.accesses
                if receiver.isDefined

                // Extract TAC
                definedMethod = contextFromId(accessContextId).method.asDefinedMethod
                method = definedMethod.definedMethod
                tacEP <- extractPropertyUB(
                    EPK(method, TACAI.key), state.addDependency((depender, definedMethod, receiver), _)
                )
                theTAC <- tacEP.tac
                defSite <- uVarForDefSites(receiver.get, theTAC.pcToIndex).definedBy

                // Extract caller context
                callers <- extractPropertyUB(
                    EPK(definedMethod, Callers.key),
                    state.addDependency((depender, definedMethod, receiver), _)
                )
                (calleeContext, _, _, _) <- callers.callContexts(definedMethod)(this).iterator
            } {
                val defPC = if (defSite < 0) defSite else theTAC.stmts(defSite).pc

                result = combine(
                    result,
                    typesProperty(field, DefinitionSite(method, defPC), depender, calleeContext, theTAC.stmts)
                )
            }

            result
        }
    }

    // TODO several field-related methods are missing here!
}
