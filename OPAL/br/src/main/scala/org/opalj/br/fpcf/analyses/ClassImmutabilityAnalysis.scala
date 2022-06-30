/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.fpcf.ELBP
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.IncrementalResult
import org.opalj.fpcf.InterimE
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBP
import org.opalj.fpcf.LUBP
import org.opalj.fpcf.MultiResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputation
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.FinalField
import org.opalj.br.fpcf.properties.ImmutableContainer
import org.opalj.br.fpcf.properties.ImmutableContainerType
import org.opalj.br.fpcf.properties.ImmutableObject
import org.opalj.br.fpcf.properties.ImmutableType
import org.opalj.br.fpcf.properties.MutableObject
import org.opalj.br.fpcf.properties.MutableObjectByAnalysis
import org.opalj.br.fpcf.properties.MutableObjectDueToUnknownSupertypes
import org.opalj.br.fpcf.properties.MutableType
import org.opalj.br.fpcf.properties.NonFinalField
import org.opalj.br.fpcf.properties.TypeImmutability

/**
 * Determines the mutability of instances of a specific class. In case the class
 * is abstract the (implicit) assumption is made that all abstract methods (if any) are/can
 * be implemented without necessarily/always requiring additional state; i.e., only the currently
 * defined fields are taken into consideration. An interfaces is always considered to be immutable.
 * If you need to know if all possible instances of an interface or some type; i.e., all instances
 * of the classes that implement the respective interface/inherit from some class are immutable,
 * you can query the [[org.opalj.br.fpcf.properties.TypeImmutability]] property.
 *
 * In case of incomplete class hierarchies or if the class hierarchy is complete, but some
 * class files are not found the sound approximation is done that the respective classes are
 * mutable.
 *
 * This analysis uses the [[org.opalj.br.fpcf.properties.FieldMutability]] property to determine
 * those fields which could be final, but which are not declared as final.
 *
 * TODO Discuss the case if a constructor calls an instance method which is overrideable (See Verifiable Functional Purity Paper for some arguements.)
 *
 * @author Michael Eichberg
 * @author Florian Kübler
 * @author Dominik Helm
 */
class ClassImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis {
    /*
     * The analysis is implemented as an incremental analysis which starts with the analysis
     * of those types which directly inherit from java.lang.Object and then propagates the
     * mutability information down the class hierarchy.
     *
     * This propagation needs to be done eagerly to ensure that all types are associated with
     * some property when the initial computation finishes and fallback properties are associated.
     */

    /**
     * Creates a result object that sets this type and all subclasses of if to the given
     * immutability rating.
     */
    @inline private[this] def createResultForAllSubtypes(
        t:            ObjectType,
        immutability: MutableObject
    ): MultiResult = {
        val allSubtypes = classHierarchy.allSubclassTypes(t, reflexive = true)
        val r = allSubtypes.map { st => new FinalEP(st, immutability) }.toSeq
        MultiResult(r)
    }

    @inline private[this] def createIncrementalResult(
        t:                   ObjectType,
        cfMutability:        EOptionP[Entity, Property],
        cfMutabilityIsFinal: Boolean,
        result:              ProperPropertyComputationResult
    ): IncrementalResult[ClassFile] = {
        var results: List[ProperPropertyComputationResult] = List(result)
        var nextComputations: List[(PropertyComputation[ClassFile], ClassFile)] = Nil
        val directSubtypes = classHierarchy.directSubtypesOf(t)
        directSubtypes.foreach { t =>
            project.classFile(t) match {
                case Some(scf) =>
                    nextComputations ::= (
                        (determineClassImmutability(t, cfMutability, cfMutabilityIsFinal, false) _, scf)
                    )
                case None =>
                    OPALLogger.warn(
                        "project configuration - object immutability analysis",
                        s"missing class file of ${t.toJava}; setting all subtypes to mutable"
                    )
                    results ::= createResultForAllSubtypes(t, MutableObjectDueToUnknownSupertypes)
            }
        }
        IncrementalResult(Results(results), nextComputations.iterator)
    }

    def doDetermineClassImmutability(e: Entity): ProperPropertyComputationResult = {
        e match {
            case t: ObjectType =>
                //this is safe
                classHierarchy.superclassType(t) match {
                    case None => Result(t, MutableObjectDueToUnknownSupertypes)
                    case Some(superClassType) =>
                        val cf = project.classFile(t) match {
                            case None =>
                                return Result(t, MutableObjectByAnalysis) //TODO consider other lattice element
                            case Some(cf) => cf
                        }

                        propertyStore(superClassType, ClassImmutability.key) match {
                            case UBP(p: MutableObject) => Result(t, p)
                            case eps: EPS[ObjectType, ClassImmutability] =>
                                determineClassImmutability(
                                    superClassType,
                                    eps,
                                    eps.isFinal,
                                    lazyComputation = true
                                )(cf)
                            case epk =>
                                determineClassImmutability(
                                    superClassType,
                                    epk,
                                    superClassMutabilityIsFinal = false,
                                    lazyComputation = true
                                )(cf)
                        }

                }
            case _ =>
                val m = e.getClass.getSimpleName+" is not an org.opalj.br.ObjectType"
                throw new IllegalArgumentException(m)
        }
    }

    private[this] object SuperClassKey

    /**
     * Determines the immutability of instances of the given class type `t`.
     *
     * @param superClassType The direct super class of the given object type `t`.
     *      Can be `null` if `superClassMutability` is `ImmutableObject`.
     * @param superClassInformation The mutability of the given super class. The mutability
     *      must not be "MutableObject"; this case has to be handled explicitly. Hence,
     *      the mutability is either unknown, immutable or (at least) conditionally immutable.
     */
    def determineClassImmutability(
        superClassType:              ObjectType,
        superClassInformation:       EOptionP[Entity, Property],
        superClassMutabilityIsFinal: Boolean,
        lazyComputation:             Boolean
    )(
        cf: ClassFile
    ): ProperPropertyComputationResult = {
        // assert(superClassMutability.isMutable.isNoOrUnknown)
        val t = cf.thisType

        var dependees = Map.empty[Entity, EOptionP[Entity, Property]]

        if (!superClassMutabilityIsFinal) {
            dependees += (SuperClassKey -> superClassInformation)
        }

        // Collect all fields for which we need to determine the effective mutability!
        var hasFieldsWithUnknownMutability = false

        val instanceFields = cf.fields.filter { f => !f.isStatic }
        dependees ++= (propertyStore(instanceFields, FieldMutability) collect {
            case FinalP(_: NonFinalField) =>
                // <=> The class is definitively mutable and therefore also all subclasses.
                if (lazyComputation)
                    return Result(t, MutableObjectByAnalysis);
                else
                    return createResultForAllSubtypes(t, MutableObjectByAnalysis);
            case ep @ InterimE(e) =>
                hasFieldsWithUnknownMutability = true
                (e, ep)
            case epk @ EPK(e: Entity, _) =>
                // <=> The mutability information is not yet available.
                hasFieldsWithUnknownMutability = true
                (e, epk)

            // case EPS(e, p: EffectivelyFinalField, _) => we can ignore effectively final fields
        }).toMap

        var minLocalImmutability: ClassImmutability =
            if (!superClassMutabilityIsFinal || hasFieldsWithUnknownMutability)
                MutableObjectByAnalysis
            else
                ImmutableContainer

        // NOTE: maxLocalImmutability does not take the super classes' mutability into account!
        var maxLocalImmutability: ClassImmutability = superClassInformation match {
            case UBP(ImmutableContainer) => ImmutableContainer
            case _                       => ImmutableObject
        }

        if (cf.fields.exists(f => !f.isStatic && f.fieldType.isArrayType)) {
            // IMPROVE We could analyze if the array is effectively final.
            // I.e., it is only initialized once (at construction time) and no reference to it
            // is passed to another object.
            maxLocalImmutability = ImmutableContainer
        }

        var fieldTypes: Set[ObjectType] = Set.empty
        if (maxLocalImmutability == ImmutableObject) {
            fieldTypes =
                // IMPROVE Use the precise type of the field (if available)!
                cf.fields.collect {
                    case f if !f.isStatic && f.fieldType.isObjectType => f.fieldType.asObjectType
                }.toSet
        }

        // For each dependent class file we have to determine the mutability
        // of instances of the respective type to determine the immutability
        // of instances of this class.
        // Basically, we have to distinguish the following cases:
        // - A field's type is mutable or conditionally immutable=>
        //            This class is conditionally immutable.
        // - A field's type is immutable =>
        //            The field is ignored.
        //            (This class is as immutable as its superclass.)
        // - A field's type is at least conditionally mutable or
        //   The immutability of the field's type is not yet known =>
        //            This type is AtLeastConditionallyImmutable
        //            We have to declare a dependency on the respective type's immutability.
        //
        // Additional handling is required w.r.t. the supertype:
        // If the supertype is Immutable =>
        //            Nothing special to do.
        // If the supertype is AtLeastConditionallyImmutable =>
        //            We have to declare a dependency on the supertype.
        // If the supertype is ConditionallyImmutable =>
        //            This type is also at most conditionally immutable.
        //
        // We furthermore have to take the mutability of the fields into consideration:
        // If a field is not effectively final =>
        //            This type is mutable.
        // If a field is effectively final =>
        //            Nothing special to do.

        val fieldTypesImmutability = propertyStore(fieldTypes, TypeImmutability.key)
        val hasMutableOrConditionallyImmutableField =
            // IMPROVE Use the precise type of the field (if available)!
            fieldTypesImmutability.exists { eOptP =>
                eOptP.hasUBP && (eOptP.ub.isMutable || eOptP.ub.isImmutableContainer)
            }

        if (hasMutableOrConditionallyImmutableField) {
            maxLocalImmutability = ImmutableContainer
        } else {
            val fieldTypesWithUndecidedMutability: Iterable[EOptionP[Entity, Property]] =
                // Recall: we don't have fields which are mutable or conditionally immutable
                fieldTypesImmutability.filterNot { eOptP =>
                    eOptP.hasUBP && eOptP.ub == ImmutableType && eOptP.isFinal
                }
            fieldTypesWithUndecidedMutability.foreach { eOptP =>
                dependees += (eOptP.e -> eOptP)
            }
        }

        if (dependees.isEmpty || minLocalImmutability == maxLocalImmutability) {
            // <=> the super classes' immutability is final
            //     (i.e., ImmutableObject or ImmutableContainer)
            // <=> all fields are (effectively) final
            // <=> the type mutability of all fields is final
            //     (i.e., ImmutableType or ImmutableContainerType)
            if (lazyComputation)
                return Result(t, maxLocalImmutability);

            return createIncrementalResult(
                t,
                FinalEP(t, maxLocalImmutability),
                cfMutabilityIsFinal = true,
                Result(t, maxLocalImmutability)
            );
        }

        def c(someEPS: SomeEPS): ProperPropertyComputationResult = {
            //[DEBUG]             val oldDependees = dependees
            someEPS match {
                // Superclass related dependencies:
                //
                case UBP(_: MutableObject) => return Result(t, MutableObjectByAnalysis);

                case LBP(ImmutableObject) => // the super class
                    dependees -= SuperClassKey

                case UBP(ImmutableContainer) => // super class is at most immutable container
                    if (someEPS.isFinal) dependees -= SuperClassKey
                    maxLocalImmutability = ImmutableContainer
                    dependees = dependees.filterNot(_._2.pk == TypeImmutability.key)

                case LBP(ImmutableContainer) => // super class is a least immutable container
                    if (minLocalImmutability != ImmutableContainer &&
                        !dependees.valuesIterator.exists(_.pk == FieldMutability.key))
                        minLocalImmutability = ImmutableContainer // Lift lower bound when possible

                case LUBP(_: MutableObject, ImmutableObject) => // No information about superclass

                // Properties related to the type of the class' fields.
                //
                case UBP(ImmutableContainerType | MutableType) =>
                    maxLocalImmutability = ImmutableContainer
                    dependees = dependees.filterNot(_._2.pk == TypeImmutability.key)

                case ELBP(e, ImmutableType) => // Immutable field type, no influence on mutability
                    dependees -= e

                case UBP(ImmutableType)    => // No information about field type

                // Field Mutability related dependencies:
                //
                case UBP(_: NonFinalField) => return Result(t, MutableObjectByAnalysis);

                case ELBP(e, _: FinalField) =>
                    dependees -= e
                    if (minLocalImmutability != ImmutableContainer &&
                        !dependees.valuesIterator.exists(_.pk != TypeImmutability.key))
                        minLocalImmutability = ImmutableContainer // Lift lower bound when possible

                case UBP(_: FinalField) => // no information about field mutability

                case _                  => throw new MatchError(someEPS) // TODO: Pattern match not exhaustive
            }

            if (someEPS.isRefinable) {
                val entity = if (someEPS.pk == ClassImmutability.key) SuperClassKey else someEPS.e
                dependees += (entity -> someEPS)
            }

            /*[DEBUG]
                assert(
                    oldDependees != dependees,
                    s"dependees are not correctly updated $e($p)\n:old=$oldDependees\nnew=$dependees"
                )
                */

            // Lift lower bound once no dependencies other than field type mutabilities are left
            if (minLocalImmutability != ImmutableContainer &&
                dependees.valuesIterator.forall(_.pk == TypeImmutability.key))
                minLocalImmutability = ImmutableContainer

            if (dependees.isEmpty || minLocalImmutability == maxLocalImmutability) {
                /*[DEBUG]
                    assert(
                        maxLocalImmutability == ConditionallyImmutableObject ||
                            maxLocalImmutability == ImmutableObject
                    )
                    assert(
                        (
                            currentSuperClassMutability == AtLeastConditionallyImmutableObject &&
                            maxLocalImmutability == ConditionallyImmutableObject
                        ) ||
                            currentSuperClassMutability == ConditionallyImmutableObject ||
                            currentSuperClassMutability == ImmutableObject,
                        s"$e: $p resulted in no dependees with unexpected "+
                            s"currentSuperClassMutability=$currentSuperClassMutability/"+
                            s"maxLocalImmutability=$maxLocalImmutability - "+
                            s"(old dependees: ${oldDependees.mkString(",")}"
                    )
                     */

                Result(t, maxLocalImmutability)

            } else {
                InterimResult(t, minLocalImmutability, maxLocalImmutability, dependees.valuesIterator.toSet, c)
            }
        }

        //[DEBUG] assert(initialImmutability.isRefinable)
        val result =
            InterimResult(t, minLocalImmutability, maxLocalImmutability, dependees.valuesIterator.toSet, c)
        if (lazyComputation)
            result
        else {
            val isFinal = dependees.isEmpty
            createIncrementalResult(
                t, EPS(t, minLocalImmutability, maxLocalImmutability), isFinal, result
            )
        }
    }
}

trait ClassImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(ClassImmutability)

    final override def uses: Set[PropertyBounds] =
        PropertyBounds.lubs(ClassImmutability, TypeImmutability, FieldMutability)

    override type InitializationData = IterableOnce[ClassFile]

    private[this] def setResultsAndComputeEntities(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): IterableOnce[ClassFile] = {
        val classHierarchy = project.classHierarchy
        import classHierarchy.allSubtypes
        import classHierarchy.rootClassTypesIterator
        import propertyStore.set
        implicit val logContext: LogContext = project.logContext

        // 1.1
        // java.lang.Object is by definition immutable.
        set(ObjectType.Object, ImmutableObject)

        // 1.2
        // All (instances of) interfaces are (by their very definition) also immutable.
        val allInterfaces = project.allClassFiles.filter(cf => cf.isInterfaceDeclaration)
        allInterfaces.map(cf => set(cf.thisType, ImmutableObject))

        // 2.
        // All classes that do not have complete superclass information are mutable
        // due to the lack of knowledge.
        // But, for classes that directly inherit from Object, but which also
        // implement unknown interface types it is possible to compute the class
        // immutability
        val unexpectedRootClassTypes = rootClassTypesIterator.filter(rt => rt ne ObjectType.Object)

        unexpectedRootClassTypes foreach { rt =>
            allSubtypes(rt, reflexive = true) foreach { ot =>
                project.classFile(ot) foreach { cf =>
                    set(cf.thisType, MutableObjectDueToUnknownSupertypes)
                }
            }
        }

        // 3.
        // Compute the initial set of classes for which we want to determine the mutability.
        var cfs: List[ClassFile] = Nil
        classHierarchy.directSubclassesOf(ObjectType.Object).iterator.
            map(ot => (ot, project.classFile(ot))).
            foreach {
                case (_, Some(cf)) => cfs ::= cf
                case (t, None) =>
                    // This handles the case where the class hierarchy is at least partially
                    // based on a pre-configured class hierarchy (*.ths file).
                    // E.g., imagine that you analyze a lib which contains a class that inherits
                    // from java.lang.Exception, but you have no knowledge about this respective
                    // class...
                    OPALLogger.warn(
                        "project configuration - object immutability analysis",
                        s"${t.toJava}'s class file is not available"
                    )
                    allSubtypes(t, reflexive = true).foreach(project.classFile(_).foreach { cf =>
                        set(cf.thisType, MutableObjectDueToUnknownSupertypes)
                    })
            }
        cfs
    }

    override def init(p: SomeProject, ps: PropertyStore): InitializationData = {
        setResultsAndComputeEntities(p, ps)
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}

/**
 * Scheduler to run the immutability analysis eagerly.
 *
 * @author Michael Eichberg
 */
object EagerClassImmutabilityAnalysis
    extends ClassImmutabilityAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, cfs: InitializationData): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(p)
        ps.scheduleEagerComputationsForEntities(cfs)(
            analysis.determineClassImmutability(
                superClassType = null,
                FinalEP(ObjectType.Object, ImmutableObject),
                superClassMutabilityIsFinal = true,
                lazyComputation = false
            )
        )
        analysis
    }
}

/**
 * Scheduler to run the immutability analysis lazily.
 *
 * @author Michael Eichberg
 */
object LazyClassImmutabilityAnalysis
    extends ClassImmutabilityAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: InitializationData
    ): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            ClassImmutability.key, analysis.doDetermineClassImmutability
        )
        analysis
    }
}
