/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br._
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.NEW
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Marks types as instantiated if their constructor is invoked. Constructors invoked by subclass
 * constructors do not result in additional instantiated types.
 * The analysis does not just looks for "new" instructions, in order to support reflection.
 *
 * This analysis is adapted from the RTA version. Instead of adding the instantiations to the type
 * set of the Project, they are added to the type set of the calling method. Which entity the type
 * is attached to depends on the call graph variant used.
 *
 *
 * TODO: Refactor this and the rta version in order to provide a common base-class.
 *
 * @author Florian Kuebler
 * @author Andreas Bauer
 */
class InstantiatedTypesAnalysis private[analyses] (
        final val project:     SomeProject,
        val setEntitySelector: TypeSetEntitySelector
) extends FPCFAnalysis {
    implicit private val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {

        // only constructors may initialize a class
        if (declaredMethod.name != "<init>")
            return NoResult;

        val callersEOptP = propertyStore(declaredMethod, Callers.key)

        val callersUB: Callers = (callersEOptP: @unchecked) match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] ⇒
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                } else {
                    eps.ub
                }
            // the method is reachable, so we analyze it!
        }

        val declaredType = declaredMethod.declaringClassType.asObjectType

        val cfOpt = project.classFile(declaredType)

        // abstract classes can never be instantiated
        cfOpt.foreach { cf ⇒
            if (cf.isAbstract)
                return NoResult;
        }

        processCallers(declaredMethod, declaredType, callersEOptP, callersUB, Set.empty)
    }

    private[this] def processCallers(
        declaredMethod: DeclaredMethod,
        declaredType:   ObjectType,
        callersEOptP:   EOptionP[DeclaredMethod, Callers],
        callersUB:      Callers,
        seenCallers:    Set[DeclaredMethod]
    ): PropertyComputationResult = {
        var newSeenCallers = seenCallers
        val partialResults = new ListBuffer[PartialResult[TypeSetEntity, InstantiatedTypes]]()
        for {
            (caller, _, _) ← callersUB.callers
            // if we already analyzed the caller, we do not need to do it twice
            // note, that this is only needed for the continuation
            if !seenCallers.contains(caller)
        } {
            processSingleCaller(declaredMethod, declaredType, caller, partialResults)

            // remember the caller so we don't process it again later
            newSeenCallers += caller
        }

        if (callersEOptP.isFinal) {
            NoResult
        } else {
            val reRegistration =
                InterimPartialResult(
                    Set(callersEOptP),
                    continuation(declaredMethod, declaredType, newSeenCallers)
                )

            Results(reRegistration, partialResults)
        }
    }

    private[this] def processSingleCaller(
        declaredMethod: DeclaredMethod,
        declaredType:   ObjectType,
        caller:         DeclaredMethod,
        partialResults: ListBuffer[PartialResult[TypeSetEntity, InstantiatedTypes]]
    ): Unit = {
        // a constructor is called by a non-constructor method, there will be an initialization.
        if (caller.name != "<init>") {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        // the constructor is called from another constructor. it is only an new instantiated
        // type if it was no super call. Thus the caller must be a subtype
        if (!classHierarchy.isSubtypeOf(caller.declaringClassType, declaredType)) {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        // actually it must be the direct subtype! -- we did the first check to return early
        project.classFile(caller.declaringClassType.asObjectType).foreach { cf ⇒
            cf.superclassType.foreach { supertype ⇒
                if (supertype != declaredType) {
                    partialResults += partialResult(declaredType, caller);
                    return ;
                }
            }
        }

        // if the caller is not available, we have to assume that it was no super call
        if (!caller.hasSingleDefinedMethod) {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        val callerMethod = caller.definedMethod

        // if the caller has no body, we have to assume that it was no super call
        if (callerMethod.body.isEmpty) {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        val supercall = INVOKESPECIAL(
            declaredType,
            isInterface = false,
            "<init>",
            declaredMethod.descriptor
        )

        val pcsOfSuperCalls = callerMethod.body.get.collectInstructionsWithPC {
            case pcAndInstr @ PCAndInstruction(_, `supercall`) ⇒ pcAndInstr
        }

        assert(pcsOfSuperCalls.nonEmpty)

        // there can be only one super call, so there must be an explicit call
        if (pcsOfSuperCalls.size > 1) {
            partialResults += partialResult(declaredType, caller);
            return ;
        }

        // there is exactly the current call as potential super call, it still might no super
        // call if the class has another constructor that calls the super. In that case
        // there must either be a new of the `declaredType` or it is a super call.
        val newInstr = NEW(declaredType)
        val hasNew = callerMethod.body.get.exists {
            case (_, i) ⇒ i == newInstr
        }
        if (hasNew) {
            partialResults += partialResult(declaredType, caller);
        }
    }

    private[this] def continuation(
        declaredMethod: DeclaredMethod,
        declaredType:   ObjectType,
        seenCallers:    Set[DeclaredMethod]
    )(someEPS: SomeEPS): PropertyComputationResult = {
        val eps = someEPS.asInstanceOf[EPS[DeclaredMethod, Callers]]
        processCallers(declaredMethod, declaredType, eps, eps.ub, seenCallers)
    }

    private def partialResult(
        declaredType: ObjectType,
        caller:       DeclaredMethod
    ): PartialResult[TypeSetEntity, InstantiatedTypes] = {

        // Subtypes of Throwable are tracked globally.
        val setEntity =
            if (classHierarchy.isSubtypeOf(declaredType, ObjectType.Throwable))
                project
            else
                setEntitySelector(caller)

        PartialResult[TypeSetEntity, InstantiatedTypes](
            setEntity,
            InstantiatedTypes.key,
            InstantiatedTypes.update(setEntity, UIDSet(declaredType))
        )
    }

    def getInstantiatedTypesUB(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes]
    ): UIDSet[ReferenceType] = {
        instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types
            case _              ⇒ UIDSet.empty
        }
    }
}

class InstantiatedTypesAnalysisScheduler(
        val selectSetEntity: TypeSetEntitySelector
) extends BasicFPCFTriggeredAnalysisScheduler {

    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes, Callers)

    override def triggeredBy: PropertyKey[Callers] = Callers.key

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new InstantiatedTypesAnalysis(p, selectSetEntity)
        ps.registerTriggeredComputation(triggeredBy, analysis.analyze)
        analysis
    }

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        assignInitialTypeSets(p, ps)
        null
    }

    def assignInitialTypeSets(p: SomeProject, ps: PropertyStore): Unit = {
        val packageIsClosed = p.get(ClosedPackagesKey)
        val declaredMethods = p.get(DeclaredMethodsKey)
        val entryPoints = p.get(InitialEntryPointsKey)
        val initialInstantiatedTypes = p.get(InitialInstantiatedTypesKey).toSet

        // While processing entry points and fields, we keep track of all array types we see, as
        // well as subtypes and lower-dimensional types. These types also need to be
        // pre-initialized. Note: This set only contains ArrayTypes whose element type is an
        // ObjectType. Arrays of primitive types can be ignored.
        val seenArrayTypes = mutable.Set[ArrayType]()

        def initialize(setEntity: TypeSetEntity, types: Traversable[ReferenceType]): Unit = {
            ps.preInitialize(setEntity, InstantiatedTypes.key) {
                case UBP(typeSet) ⇒
                    InterimEUBP(setEntity, typeSet.updated(types))
                case _: EPK[_, _] ⇒
                    InterimEUBP(setEntity, InstantiatedTypes(UIDSet(types.toSeq: _*)))
                case eps ⇒
                    sys.error(s"unexpected property: $eps")
            }
        }

        // Some cooperative analyses originally meant for RTA may require the global type set
        // to be pre-initialized. For that purpose, an empty type set is sufficient.
        initialize(p, Traversable.empty)

        def isRelevantArrayType(rt: Type): Boolean =
            rt.isArrayType && rt.asArrayType.elementType.isObjectType

        // For each method which is also an entry point, we assume that the caller has passed all subtypes of the
        // method's parameter types to the method.
        for (
            ep ← entryPoints;
            dm = declaredMethods(ep)
        ) {
            val typeFilters = mutable.Set[ReferenceType]()
            val arrayTypeAssignments = mutable.Set[ArrayType]()

            if (!dm.definedMethod.isStatic) {
                typeFilters += dm.declaringClassType
            }

            for (pt ← dm.descriptor.parameterTypes) {
                if (pt.isObjectType) {
                    typeFilters += pt.asObjectType
                } else if (isRelevantArrayType(pt)) {
                    seenArrayTypes += pt.asArrayType

                    val dim = pt.asArrayType.dimensions
                    val et = pt.asArrayType.elementType.asObjectType
                    p.classHierarchy.allSubtypesForeachIterator(et, reflexive = true).foreach { subtype ⇒
                        if (initialInstantiatedTypes.contains(subtype)) {
                            val at = ArrayType(dim, subtype)
                            arrayTypeAssignments += at
                        }
                    }
                }
            }

            // Initial assignments of ObjectTypes
            val objectTypeAssignments = initialInstantiatedTypes.filter(iit ⇒ typeFilters.exists(tf ⇒
                p.classHierarchy.isSubtypeOf(iit, tf)))

            val initialAssignment = arrayTypeAssignments ++ objectTypeAssignments

            val dmSetEntity = selectSetEntity(dm)

            initialize(dmSetEntity, initialAssignment)
        }

        // Returns true if the field's type indicates that the field should be pre-initialized.
        def fieldIsRelevant(f: Field): Boolean = {
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
            iit ← initialInstantiatedTypes;
            // Only object types should be initially instantiated.
            ot = iit.asObjectType
        ) {
            // Assign initial types to all accessable fields.
            p.classFile(ot) match {
                case Some(cf) ⇒
                    for (f ← cf.fields if fieldIsRelevant(f) && f.isNotFinal && fieldIsAccessible(f)) {
                        val fieldType = f.fieldType.asReferenceType

                        val initialAssignments = fieldType match {
                            case ot: ObjectType ⇒
                                initialInstantiatedTypes.filter(
                                    p.classHierarchy.isSubtypeOf(_, ot)
                                )

                            case at: ArrayType ⇒
                                seenArrayTypes += at

                                val dim = at.dimensions
                                val et = at.elementType.asObjectType
                                p.classHierarchy.allSubtypes(et, reflexive = true)
                                    .intersect(initialInstantiatedTypes).map(
                                        ArrayType(dim, _)
                                    )

                        }

                        val fieldSetEntity = selectSetEntity(f)
                        initialize(fieldSetEntity, initialAssignments)
                    }
                case None ⇒
                    // Sanity check. If some other class than String is not available, there is probably a bug here.
                    if (ot != ObjectType.String)
                        sys.error(s"Class file of $ot is not available.")
            }
        }

        // Next, process all ArrayTypes that have been seen while processing entry points and fields,
        // and initialize their type sets.

        // Remember which ArrayTypes were processed, so we don't do it twice.
        val initializedArrayTypes = mutable.Set[ArrayType]()

        def initializeArrayType(at: ArrayType): Unit = {
            // If this type has already been initialized, we skip it.
            if (initializedArrayTypes.contains(at)) {
                return ;
            }

            initializedArrayTypes += at

            val et = at.elementType.asObjectType
            val subtypes = p.classHierarchy.allSubtypes(et, reflexive = true).intersect(initialInstantiatedTypes)

            val dim = at.dimensions
            if (dim > 1) {
                // Initialize multidimensional ArrayType. E.g., if at == A[][] and A is a supertype of A1,
                // we need to assign A[] and A1[] to the type set of A[][].
                val assignedArrayTypes = subtypes.map(ArrayType(dim - 1, _))
                initialize(at, assignedArrayTypes)

                // After that, we also need to initialize the ArrayTypes which were just assigned. It is possible
                // that these were types which were not initially seen when processing entry points and fields.
                assignedArrayTypes foreach initializeArrayType
            } else {
                // If dim == 1, we just need to assign the "pure" ObjectTypes to the ArrayType.
                initialize(at, subtypes)
            }
        }

        seenArrayTypes foreach initializeArrayType
    }
}
