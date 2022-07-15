/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.br.Field
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.ArrayType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.Type
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.NoContext
import org.opalj.br.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.properties.cg.Callers

/**
 * Provides initial points to sets for the parameters of entry point methods, fields and arrays as
 * required for library analysis.
 *
 * Note: Does not mark public methods of instantiated types as entry points which would also be
 * required for full library analysis.
 *
 * @author Dominik Helm
 */
abstract class LibraryPointsToAnalysis( final val project: SomeProject)
    extends PointsToAnalysisBase {

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        NoResult
    }

    def assignInitialPointsToSets(p: SomeProject, ps: PropertyStore): Unit = {
        val packageIsClosed = p.get(ClosedPackagesKey)
        val declaredMethods = p.get(DeclaredMethodsKey)
        val entryPoints = p.get(InitialEntryPointsKey)
        val formalParameters = p.get(VirtualFormalParametersKey)
        val initialInstantiatedTypes =
            UIDSet[ReferenceType](p.get(InitialInstantiatedTypesKey).toSeq: _*)

        // While processing entry points and fields, we keep track of all array types we see, as
        // well as subtypes and lower-dimensional types. These types also need to be
        // pre-initialized. Note: This set only contains ArrayTypes whose element type is an
        // ObjectType. Arrays of primitive types can be ignored.
        val seenArrayTypes = UIDSet.newBuilder[ArrayType]

        def createExternalAllocation(tpe: ReferenceType): PointsToSet = {
            createPointsToSet(0xFFFF, NoContext.asInstanceOf[ContextType], tpe, false)
        }

        def initialize(param: Entity, types: UIDSet[ReferenceType]): Unit = {
            val pts = types.foldLeft(emptyPointsToSet) { (all, tpe) =>
                all.included(createExternalAllocation(tpe))
            }
            ps.preInitialize(param, pointsToPropertyKey) {
                case UBP(oldPts) =>
                    InterimEUBP(param, oldPts.included(pts))
                case _: EPK[_, _] =>
                    InterimEUBP(param, pts)
                case eps =>
                    sys.error(s"unexpected property: $eps")
            }
        }

        def isRelevantArrayType(rt: Type): Boolean =
            rt.isArrayType && rt.asArrayType.elementType.isObjectType

        // For each method which is also an entry point, we assume that the caller has passed all
        // subtypes of the method's parameter types to the method.
        for (
            ep <- entryPoints;
            dm = declaredMethods(ep)
        ) {
            val fps = formalParameters(dm)
            val context = typeProvider.newContext(dm)

            if (!dm.definedMethod.isStatic) {
                if (initialInstantiatedTypes.contains(dm.declaringClassType))
                    initialize(getFormalParameter(0, fps, context), UIDSet(dm.declaringClassType))
            }

            for (i <- 0 until dm.descriptor.parametersCount) {
                val pt = dm.descriptor.parameterType(i)
                val fp = getFormalParameter(i + 1, fps, context)

                if (pt.isObjectType) {
                    val validTypes = initialInstantiatedTypes.filter(iit =>
                        classHierarchy.isSubtypeOf(iit, pt.asObjectType))
                    initialize(fp, validTypes)
                } else if (isRelevantArrayType(pt)) {
                    seenArrayTypes += pt.asArrayType

                    val dim = pt.asArrayType.dimensions
                    val et = pt.asArrayType.elementType.asObjectType
                    if (initialInstantiatedTypes.contains(et)) {
                        initialize(fp, UIDSet(ArrayType(dim, et)))
                    }
                }
            }
        }

        // Returns true if the field's type indicates that the field should be pre-initialized.
        @inline def fieldIsRelevant(f: Field): Boolean = {
            // Only fields which are ArrayType or ObjectType are relevant.
            f.fieldType.isReferenceType &&
                // If the field is an ArrayType, then the array's element type must be an ObjectType.
                // In other words: We don't care about arrays of primitive types (e.g. int[]) which
                // do not have to be pre-initialized.
                (!f.fieldType.isArrayType || f.fieldType.asArrayType.elementType.isObjectType)
        }

        // Returns true if a field can be written by the user of a library containing that field.
        def fieldIsAccessible(f: Field): Boolean = {
            // Public fields can always be accessed.
            f.isPublic ||
                // Protected fields can only be accessed by subclasses. In that case, the library
                // user can create a subclass of the type containing the field and add a setter method.
                // This only applies if the field's type can be extended in the first place.
                (f.isProtected && !f.classFile.isEffectivelyFinal) ||
                // If the field is package private, it can only be written if the package is
                // open for modification. In that case, the library user can put a method
                // writing that field into the field's type's namespace.
                (f.isPackagePrivate && !packageIsClosed(f.classFile.thisType.packageName))
        }

        for (
            iit <- initialInstantiatedTypes;
            // Only object types should be initially instantiated.
            ot = iit.asObjectType
        ) {
            // Assign initial types to all accessible fields.
            p.classFile(ot) match {
                case Some(cf) =>
                    for (f <- cf.fields if f.isNotFinal && fieldIsRelevant(f) && fieldIsAccessible(f)) {
                        val fieldType = f.fieldType.asReferenceType

                        val initialAssignments = if (fieldType.isObjectType) {
                            val ot = fieldType.asObjectType
                            initialInstantiatedTypes.foldLeft(UIDSet.newBuilder[ReferenceType]) {
                                (assignments, iit) =>
                                    if (classHierarchy.isSubtypeOf(iit, ot)) {
                                        assignments += iit
                                    }
                                    assignments
                            }.result()
                        } else {
                            val at = fieldType.asArrayType
                            seenArrayTypes += at
                            val dim = at.dimensions
                            val et = at.elementType.asObjectType
                            if (initialInstantiatedTypes.contains(et)) {
                                UIDSet[ReferenceType](ArrayType(dim, iit))
                            } else {
                                UIDSet.empty[ReferenceType]
                            }
                        }

                        if (f.isStatic) initialize(f, initialAssignments)
                        else {
                            createExternalAllocation(cf.thisType).forNewestNElements(1) { as =>
                                initialize((as, f), initialAssignments)
                            }
                        }
                    }
                case None =>
                // Nothing to do here, no classfile => no fields
            }
        }

        // Next, process all ArrayTypes that have been seen while processing entry points and fields,
        // and initialize their type sets.

        // Remember which ArrayTypes were processed, so we don't do it twice.
        val initializedArrayTypes = new java.util.HashSet[ArrayType]()

        def initializeArrayType(at: ArrayType): Unit = {
            // If this type has already been initialized, we skip it.
            if (initializedArrayTypes.contains(at)) {
                return ;
            }

            initializedArrayTypes.add(at)

            val et = at.elementType.asObjectType
            val allSubtypes = p.classHierarchy.allSubtypes(et, reflexive = true)
            val subtypes =
                initialInstantiatedTypes.foldLeft(UIDSet.newBuilder[ReferenceType]) { (builder, iit) =>
                    if (allSubtypes.contains(iit.asObjectType)) {
                        builder += iit
                    }
                    builder
                }.result()

            val dim = at.dimensions
            if (dim > 1) {
                // Initialize multidimensional ArrayType. E.g., if at == A[][] and A is a supertype of A1,
                // we need to assign A[] and A1[] to the type set of A[][].
                val assignedArrayTypes: UIDSet[ArrayType] = UIDSet.fromSpecific(subtypes.map(ArrayType(dim - 1, _)))
                initialize(ArrayEntity(at), assignedArrayTypes.asInstanceOf[UIDSet[ReferenceType]])

                // After that, we also need to initialize the ArrayTypes which were just assigned. It is possible
                // that these were types which were not initially seen when processing entry points and fields.
                assignedArrayTypes foreach initializeArrayType
            } else {
                // If dim == 1, we just need to assign the "pure" ObjectTypes to the ArrayType.
                initialize(ArrayEntity(at), subtypes)
            }
        }

        seenArrayTypes.result() foreach initializeArrayType
    }

}

trait LibraryPointsToAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    val propertyKind: PropertyMetaInformation
    val createAnalysis: SomeProject => LibraryPointsToAnalysis

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        TypeProviderKey,
        ClosedPackagesKey, DeclaredMethodsKey, InitialEntryPointsKey, VirtualFormalParametersKey,
        InitialInstantiatedTypesKey
    )

    override type InitializationData = LibraryPointsToAnalysis

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers)

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(propertyKind)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, analysis: LibraryPointsToAnalysis): FPCFAnalysis = {
        analysis
    }

    override def init(p: SomeProject, ps: PropertyStore): LibraryPointsToAnalysis = {
        val analysis = createAnalysis(p)
        analysis.assignInitialPointsToSets(p, ps)
        analysis
    }
    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}

object TypeBasedLibraryPointsToAnalysisScheduler extends LibraryPointsToAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = TypeBasedPointsToSet
    override val createAnalysis: SomeProject => LibraryPointsToAnalysis =
        new LibraryPointsToAnalysis(_) with TypeBasedAnalysis
}

object AllocationSiteBasedLibraryPointsToAnalysisScheduler extends LibraryPointsToAnalysisScheduler {
    override val propertyKind: PropertyMetaInformation = AllocationSitePointsToSet
    override val createAnalysis: SomeProject => LibraryPointsToAnalysis =
        new LibraryPointsToAnalysis(_) with AllocationSiteBasedAnalysis
}