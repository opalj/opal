/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
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
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.ClassImmutability_new
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableField
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.ImmutableType
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.MutableField
import org.opalj.br.fpcf.properties.MutableType
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableField
import org.opalj.br.fpcf.properties.TypeImmutability

/**
 *
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
 * This analysis uses the [[org.opalj.br.fpcf.properties.FieldImmutability]] property to determine
 * those fields which could be final, but which are not declared as final.
 *
 * TODO Discuss the case if a constructor calls an instance method which is overrideable (See Verifiable Functional Purity Paper for some arguements.)
 *
 * @author Michael Eichberg
 * @author Florian Kübler
 * @author Dominik Helm
 * @author Tobias Peter Roth
 *
 */
class LxClassImmutabilityAnalysis_new(val project: SomeProject) extends FPCFAnalysis {
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
        immutability: ClassImmutability_new //MutableObject
    ): MultiResult = {
        val allSubtypes = classHierarchy.allSubclassTypes(t, reflexive = true)
        val r = allSubtypes.map { st ⇒
            new FinalEP(st, immutability)
        }.toSeq
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
        directSubtypes.foreach { t ⇒
            project.classFile(t) match {
                case Some(scf) ⇒
                    nextComputations ::= (
                        (
                            determineClassImmutability_new(t, cfMutability, cfMutabilityIsFinal, false) _,
                            scf
                        )
                    )
                case None ⇒
                    OPALLogger.warn(
                        "project configuration - object immutability analysis",
                        s"missing class file of ${t.toJava}; setting all subtypes to mutable"
                    )
                    results ::= createResultForAllSubtypes(t, MutableClass) //MutableObjectDueToUnknownSupertypes)
            }
        }
        IncrementalResult(Results(results), nextComputations.iterator)
    }

    def doDetermineClassImmutability_new(e: Entity): ProperPropertyComputationResult = {
        e match {
            case t: ObjectType ⇒
                //this is safe
                classHierarchy.superclassType(t) match {
                    case None ⇒ Result(t, MutableClass) //MutableObjectDueToUnknownSupertypes)
                    case Some(superClassType) ⇒
                        val cf = project.classFile(t) match {
                            case None ⇒
                                return Result(t, MutableClass) // MutableObjectByAnalysis) //TODO consider other lattice element
                            case Some(cf) ⇒ cf
                        }

                        propertyStore(superClassType, ClassImmutability_new.key) match {
                            case UBP(MutableClass) ⇒ //MutableObject) ⇒
                                Result(t, MutableClass)
                            case eps: EPS[ObjectType, ClassImmutability_new] ⇒
                                determineClassImmutability_new(
                                    superClassType,
                                    eps,
                                    eps.isFinal,
                                    lazyComputation = true
                                )(cf)
                            case epk ⇒
                                determineClassImmutability_new(
                                    superClassType,
                                    epk,
                                    superClassMutabilityIsFinal = false,
                                    lazyComputation = true
                                )(cf)
                        }

                }
            case _ ⇒
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
    def determineClassImmutability_new(
        superClassType:              ObjectType,
        superClassInformation:       EOptionP[Entity, Property],
        superClassMutabilityIsFinal: Boolean,
        lazyComputation:             Boolean
    )(
        cf: ClassFile
    ): ProperPropertyComputationResult = {
        // assert(superClassMutability.isMutable.isNoOrUnknown)

        //---generic classes handling
        //printf(cf.asVirtualClass.asClassFile.classSignature.get.toString())
        //cf.asClassFile.classSignature.get.formalTypeParameters.
        //---------------------------------------------

        val t = cf.thisType

        var dependees = Map.empty[Entity, EOptionP[Entity, Property]]

        if (!superClassMutabilityIsFinal) {
            dependees += (SuperClassKey -> superClassInformation)
        }

        // Collect all fields for which we need to determine the effective mutability!
        var hasFieldsWithUnknownMutability = false

        val instanceFields = cf.fields.filter { f ⇒
            !f.isStatic
        }
        var hasShallowImmutableFields = false
        var hasDependentImmutableFields = false
        val fieldsPropertyStoreInformation = propertyStore(instanceFields, FieldImmutability)
        fieldsPropertyStoreInformation.foreach(
            f ⇒
                f match {
                    case FinalP(MutableField) ⇒ {
                        if (lazyComputation)
                            return Result(t, MutableClass);
                        else
                            return createResultForAllSubtypes(t, MutableClass);
                    }
                    case FinalP(ShallowImmutableField)   ⇒ hasShallowImmutableFields = true
                    case FinalP(DependentImmutableField) ⇒ hasDependentImmutableFields = true
                    case ep @ InterimE(e) ⇒
                        hasFieldsWithUnknownMutability = true
                        dependees += (e -> ep)
                    case epk @ EPK(e: Entity, _) ⇒
                        // <=> The mutability information is not yet available.
                        hasFieldsWithUnknownMutability = true
                        dependees += (e -> epk)
                    case _ ⇒
                        if (lazyComputation) //TODO check
                            return Result(t, MutableClass);
                        else
                            return createResultForAllSubtypes(t, MutableClass);
                }
        )

        /**
         * dependees ++= (propertyStore(instanceFields, FieldImmutability) collect {
         * case FinalP(MutableField) => //NonFinalField) ⇒
         * // <=> The class is definitively mutable and therefore also all subclasses.
         * if (lazyComputation)
         * return Result(t, MutableClass); //MutableObjectByAnalysis); //
         * else
         * return createResultForAllSubtypes(t, MutableClass); //MutableObjectByAnalysis);
         * case ep @ InterimE(e) =>
         * hasFieldsWithUnknownMutability = true
         * (e, ep)
         * case epk @ EPK(e: Entity, _) =>
         * // <=> The mutability information is not yet available.
         * hasFieldsWithUnknownMutability = true
         * (e, epk)
         *
         * // case EPS(e, p: EffectivelyFinalField, _) => we can ignore effectively final fields
         * }).toMap *
         */
        var minLocalImmutability: ClassImmutability_new =
            if (!superClassMutabilityIsFinal || hasFieldsWithUnknownMutability)
                MutableClass //MutableObjectByAnalysis
            else
                ShallowImmutableClass //ImmutableContainer

        // NOTE: maxLocalImmutability does not take the super classes' mutability into account!
        var maxLocalImmutability: ClassImmutability_new = superClassInformation match {
            case UBP(ShallowImmutableClass) ⇒ ShallowImmutableClass //ImmutableContainer
            case _                          ⇒ DeepImmutableClass // ImmutableObject
        }

        if (hasDependentImmutableFields) {
            maxLocalImmutability = DependentImmutableClass
        } else if (hasShallowImmutableFields) {
            maxLocalImmutability = ShallowImmutableClass //ImmutableContainer
        }

        if (cf.fields.exists(f ⇒ !f.isStatic && f.fieldType.isArrayType)) {
            // IMPROVE We could analyze if the array is effectively final.
            // I.e., it is only initialized once (at construction time) and no reference to it
            // is passed to another object.
            maxLocalImmutability = ShallowImmutableClass //ImmutableContainer
        }

        //--var fieldTypes: Set[ObjectType] = Set.empty
        //--if (maxLocalImmutability == DeepImmutableClass) { //ImmutableObject) {
        //--    fieldTypes = // IMPROVE Use the precise type of the field (if available)!
        //        cf.fields.collect {
        //           case f if !f.isStatic && f.fieldType.isObjectType ⇒ f.fieldType.asObjectType
        //        }.toSet
        //}

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

        //val fieldTypesImmutability = propertyStore(fieldTypes, TypeImmutability.key)
        //val hasMutableOrConditionallyImmutableField =
        // IMPROVE Use the precise type of the field (if available)!
        //--    fieldTypesImmutability.exists { eOptP ⇒
        //--        eOptP.hasUBP && (eOptP.ub.isMutable || eOptP.ub.isImmutableContainer)
        //--    }

        if (hasShallowImmutableFields) {
            maxLocalImmutability = ShallowImmutableClass //ImmutableContainer
        } //else {
        //val fieldTypesWithUndecidedMutability: Traversable[EOptionP[Entity, Property]] =
        // Recall: we don't have fields which are mutable or conditionally immutable
        /**
         * fieldTypesImmutability.filterNot { eOptP =>
         * eOptP.hasUBP && eOptP.ub == ImmutableType && eOptP.isFinal
         * }
         * fieldTypesWithUndecidedMutability.foreach { eOptP =>
         * dependees += (eOptP.e -> eOptP)
         * }*
         */
        //}

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
            dependees = dependees.filter(_._1 ne someEPS.e)
            println("someEPS: "+someEPS)
            someEPS match {
                // Superclass related dependencies:
                //
                case UBP(MutableClass) ⇒ // MutableObject) ⇒
                    return Result(t, MutableClass); //MutableObjectByAnalysis);

                case LBP(DeepImmutableClass) ⇒ //_:ImmutableObject) ⇒ // the super class
                    dependees -= SuperClassKey

                case UBP(ShallowImmutableClass) ⇒ //ImmutableContainer) ⇒ // super class is at most immutable container
                    if (someEPS.isFinal) dependees -= SuperClassKey
                    maxLocalImmutability = ShallowImmutableClass //ImmutableContainer
                    dependees = dependees.filterNot(_._2.pk == TypeImmutability.key)

                case LBP(ShallowImmutableClass) ⇒ //ImmutableContainer) ⇒ // super class is a least immutable container
                    if (minLocalImmutability != DeepImmutableClass && //ImmutableContainer &&
                        !dependees.valuesIterator.exists(_.pk == FieldImmutability.key))
                        minLocalImmutability = ShallowImmutableClass //ImmutableContainer // Lift lower bound when possible

                case LUBP(MutableClass, DeepImmutableClass) ⇒ //_: MutableObject, ImmutableObject) ⇒ // No information about superclass

                // Properties related to the type of the class' fields.
                //
                case UBP(ShallowImmutableClass | MutableClass) ⇒ //ImmutableContainerType | MutableType) ⇒
                    maxLocalImmutability = ShallowImmutableClass //ImmutableContainer
                    dependees = dependees.filterNot(_._2.pk == TypeImmutability.key)

                case ELBP(e, ImmutableType) ⇒ // Immutable field type, no influence on mutability
                    dependees -= e

                case UBP(ImmutableType)              ⇒ // No information about field type

                case FinalP(MutableType)             ⇒ Result(t, MutableClass) //TODO check

                case FinalP(DependentImmutableField) ⇒ maxLocalImmutability = DependentImmutableClass
                // Field Mutability related dependencies:
                //

                case FinalP(ShallowImmutableField) ⇒ {
                    maxLocalImmutability = ShallowImmutableClass
                }
                case FinalP(MutableField) ⇒ return Result(t, MutableClass);

                case UBP(MutableField) ⇒ //_: NonFinalField) ⇒
                    return Result(t, MutableClass); //MutableObjectByAnalysis);

                case ELBP(e, ShallowImmutableField | DeepImmutableField) ⇒ //FinalField) =>
                    dependees -= e
                    if (minLocalImmutability != ShallowImmutableClass && // ImmutableContainer &&
                        !dependees.valuesIterator.exists(_.pk != TypeImmutability.key))
                        minLocalImmutability = ShallowImmutableClass //ImmutableContainer // Lift lower bound when possible

                case UBP(ShallowImmutableField | DeepImmutableField) ⇒ //_: FinalField) ⇒ // no information about field mutability

                case _                                               ⇒ Result(t, MutableClass) //TODO check

            }

            if (someEPS.isRefinable) {
                val entity = if (someEPS.pk == ClassImmutability_new.key) SuperClassKey else someEPS.e
                dependees += (entity -> someEPS)
            }

            /*[DEBUG]
                assert(
                    oldDependees != dependees,
                    s"dependees are not correctly updated $e($p)\n:old=$oldDependees\nnew=$dependees"
                )
       */

            // Lift lower bound once no dependencies other than field type mutabilities are left
            if (minLocalImmutability != ShallowImmutableClass && //ImmutableContainer &&
                dependees.valuesIterator.forall(_.pk == TypeImmutability.key))
                minLocalImmutability = ShallowImmutableClass //ImmutableContainer

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
                InterimResult(t, minLocalImmutability, maxLocalImmutability, dependees.values, c)
            }
        }

        //[DEBUG] assert(initialImmutability.isRefinable)
        println("minLocalImmutability: "+minLocalImmutability)
        println("maxLocalImmutability: "+maxLocalImmutability)
        val result =
            InterimResult(t, minLocalImmutability, maxLocalImmutability, dependees.values, c)
        if (lazyComputation)
            result
        else {
            val isFinal = dependees.isEmpty
            createIncrementalResult(
                t,
                EPS(t, minLocalImmutability, maxLocalImmutability),
                isFinal,
                result
            )
        }
    }
}

trait ClassImmutabilityAnalysisScheduler_new extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(ClassImmutability_new)

    final override def uses: Set[PropertyBounds] =
        PropertyBounds.lubs(ClassImmutability_new, TypeImmutability, FieldImmutability)

    override type InitializationData = TraversableOnce[ClassFile]

    private[this] def setResultsAndComputeEntities(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): TraversableOnce[ClassFile] = {
        val classHierarchy = project.classHierarchy
        import classHierarchy.allSubtypes
        import classHierarchy.rootClassTypesIterator
        import propertyStore.set
        implicit val logContext: LogContext = project.logContext

        // 1.1
        // java.lang.Object is by definition immutable.
        set(ObjectType.Object, DeepImmutableClass) //ImmutableObject)

        // 1.2
        // All (instances of) interfaces are (by their very definition) also immutable.
        val allInterfaces = project.allClassFiles.filter(cf ⇒ cf.isInterfaceDeclaration)
        allInterfaces.map(cf ⇒ set(cf.thisType, DeepImmutableClass)) //ImmutableObject))

        // 2.
        // All classes that do not have complete superclass information are mutable
        // due to the lack of knowledge.
        // But, for classes that directly inherit from Object, but which also
        // implement unknown interface types it is possible to compute the class
        // immutability
        val unexpectedRootClassTypes = rootClassTypesIterator.filter(rt ⇒ rt ne ObjectType.Object)

        unexpectedRootClassTypes foreach { rt ⇒
            allSubtypes(rt, reflexive = true) foreach { ot ⇒
                project.classFile(ot) foreach { cf ⇒
                    set(cf.thisType, MutableClass) //MutableObjectDueToUnknownSupertypes)
                }
            }
        }

        // 3.
        // Compute the initial set of classes for which we want to determine the mutability.
        var cfs: List[ClassFile] = Nil
        classHierarchy
            .directSubclassesOf(ObjectType.Object)
            .toIterator
            .map(ot ⇒ (ot, project.classFile(ot)))
            .foreach {
                case (_, Some(cf)) ⇒ cfs ::= cf
                case (t, None) ⇒
                    // This handles the case where the class hierarchy is at least partially
                    // based on a pre-configured class hierarchy (*.ths file).
                    // E.g., imagine that you analyze a lib which contains a class that inherits
                    // from java.lang.Exception, but you have no knowledge about this respective
                    // class...
                    OPALLogger.warn(
                        "project configuration - object immutability analysis",
                        s"${t.toJava}'s class file is not available"
                    )
                    allSubtypes(t, reflexive = true).foreach(project.classFile(_).foreach { cf ⇒
                        set(cf.thisType, MutableClass) //MutableObjectDueToUnknownSupertypes)
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
 * @author Tobias Peter Roth
 * @author Michael Eichberg
 */
object EagerLxClassImmutabilityAnalysis_new
    extends ClassImmutabilityAnalysisScheduler_new
    with FPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, cfs: InitializationData): FPCFAnalysis = {
        val analysis = new LxClassImmutabilityAnalysis_new(p)
        ps.scheduleEagerComputationsForEntities(cfs)(
            analysis.determineClassImmutability_new(
                superClassType = null,
                FinalEP(ObjectType.Object, DeepImmutableClass), //ImmutableObject),
                superClassMutabilityIsFinal = true,
                lazyComputation = false
            )
        )
        analysis
    }
}

/**
 * Scheduler to run the immutability analysis lazily.
 * @author Tobias Peter Roth
 * @author Michael Eichberg
 */
object LazyLxClassImmutabilityAnalysis_new
    extends ClassImmutabilityAnalysisScheduler_new
    with FPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: InitializationData
    ): FPCFAnalysis = {
        val analysis = new LxClassImmutabilityAnalysis_new(p)
        ps.registerLazyPropertyComputation(
            ClassImmutability_new.key,
            analysis.doDetermineClassImmutability_new
        )
        analysis
    }
}
