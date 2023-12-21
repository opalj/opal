/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses
package immutability

import scala.collection.immutable.SortedSet

import org.opalj.br.ClassFile
import org.opalj.br.ClassSignature
import org.opalj.br.ClassTypeSignature
import org.opalj.br.FormalTypeParameter
import org.opalj.br.ObjectType
import org.opalj.br.SimpleClassTypeSignature
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.immutability.ClassImmutability
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableClass
import org.opalj.br.fpcf.properties.immutability.DependentlyImmutableField
import org.opalj.br.fpcf.properties.immutability.FieldImmutability
import org.opalj.br.fpcf.properties.immutability.MutableClass
import org.opalj.br.fpcf.properties.immutability.MutableField
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.NonTransitivelyImmutableField
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableClass
import org.opalj.br.fpcf.properties.immutability.TransitivelyImmutableField
import org.opalj.fpcf.ELBP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
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
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * Determines the immutability of instances of a specific class. In the case of an abstract class
 * the (implicit) assumption is made that all abstract methods (if any) are/can
 * be implemented without necessarily/always requiring additional state; i.e., only the currently
 * defined fields are taken into consideration. An interfaces is always considered to be transitively immutable.
 * If you need to know if all possible instances of an interface or some type; i.e., all instances
 * of the classes that implement the respective interface/inherit from some class are transitively immutable,
 * you can query the [[TypeImmutability]] property.
 *
 * In case of incomplete class hierarchies or if the class hierarchy is complete, but some
 * class files are not found the sound approximation is done that the respective classes are
 * mutable.
 *
 * This analysis uses the [[FieldImmutability]] property to determine the immutability of a class.
 *
 * TODO Discuss the case if a constructor calls an instance method which is overrideable (See Verifiable Functional Purity Paper for some arguments.)
 *
 * @author Michael Eichberg
 * @author Florian Kübler
 * @author Dominik Helm
 * @author Tobias Roth
 */
class ClassImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis {
    /*
     * The analysis is implemented as an incremental analysis which starts with the analysis
     * of those types which directly inherit from java.lang.Object and then propagates the
     * immutability information down the class hierarchy.
     *
     * This propagation needs to be done eagerly to ensure that all types are associated with
     * some property when the initial computation finishes and fallback properties are associated.
     */

    /**
     * Creates a result object that sets this type and all subclasses of if to the given immutability rating.
     */
    @inline private[this] def createResultForAllSubtypes(
        t:            ObjectType,
        immutability: ClassImmutability
    ): MultiResult = {
        val allSubtypes = classHierarchy.allSubclassTypes(t, reflexive = true)
        val r = allSubtypes.map { st => new FinalEP(st, immutability) }.toSeq
        MultiResult(r)
    }
    @inline private[this] def createIncrementalResult(
        t:                     ObjectType,
        cfImmutability:        EOptionP[Entity, Property],
        cfImmutabilityIsFinal: Boolean,
        result:                ProperPropertyComputationResult
    ): IncrementalResult[ClassFile] = {
        var results: List[ProperPropertyComputationResult] = List(result)
        var nextComputations: List[(PropertyComputation[ClassFile], ClassFile)] = Nil
        val directSubtypes = classHierarchy.directSubtypesOf(t)
        directSubtypes.foreach { t =>
            project.classFile(t) match {
                case Some(scf) =>
                    nextComputations ::= (
                        (
                            determineClassImmutability(t, cfImmutability, cfImmutabilityIsFinal,
                                lazyComputation = false), scf
                        )
                    )
                case None =>
                    OPALLogger.warn(
                        "project configuration - class immutability analysis",
                        s"missing class file of ${t.toJava}; setting all subtypes to mutable"
                    )
                    results ::= createResultForAllSubtypes(t, MutableClass)
            }
        }
        IncrementalResult(Results(results), nextComputations.iterator)
    }

    def determineGenericTypeBounds(classFile: ClassFile): Set[(String, String)] = {
        var genericTypeBounds: Set[(String, String)] = Set.empty
        classFile.attributes.toList.collectFirst({
            case ClassSignature(typeParameters, _, _) => typeParameters.collect({
                case ftp @ FormalTypeParameter(_, _, _) => ftp
            })
                .foreach {
                    case FormalTypeParameter(identifier, classBound, _) => classBound match {

                        case Some(ClassTypeSignature(_, SimpleClassTypeSignature(simpleName, _), _)) =>
                            genericTypeBounds += ((identifier, simpleName))

                        case _ =>
                    }

                }
        })
        genericTypeBounds
    }

    /*
     * If the type is transitively immutable the class itself is also transitively immutable.
     */
    val defaultTransitivelyImmutableTypes = project.config.getStringList(
        "org.opalj.fpcf.analyses.TypeImmutabilityAnalysis.defaultTransitivelyImmutableTypes"
    ).toArray().toList.map(s => ObjectType(s.asInstanceOf[String])).toSet

    def doDetermineClassImmutability(e: Entity): ProperPropertyComputationResult = {
        e match {
            case t: ObjectType =>
                if (defaultTransitivelyImmutableTypes.contains(t.asObjectType))
                    return Result(t, TransitivelyImmutableClass)
                // this is safe
                classHierarchy.superclassType(t) match {
                    case None => Result(t, MutableClass);
                    case Some(superClassType) =>
                        val cf = project.classFile(t) match {
                            case None =>
                                return Result(t, MutableClass); // TODO consider other lattice element
                            case Some(cf) => cf
                        }

                        propertyStore(superClassType, ClassImmutability.key) match {
                            case UBP(MutableClass) =>
                                Result(t, MutableClass)
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
                                    superClassImmutabilityIsFinal = false,
                                    lazyComputation = true
                                )(cf)
                        }
                }
            case _ =>
                val m = e.getClass.getSimpleName + " is not an org.opalj.br.ObjectType"
                throw new IllegalArgumentException(m)
        }
    }

    private[this] object SuperClassKey

    /**
     * Determines the immutability of instances of the given class type `t`.
     *
     * @param superClassType The direct super class of the given object type `t`.
     *      Can be `null` if `superClassImmutability` is `TransitivelyImmutable`.
     * @param superClassInformation The mutability of the given super class. The mutability
     *      must not be "MutableObject"; this case has to be handled explicitly. Hence,
     *      the mutability is either unknown, immutable or (at least) conditionally immutable.
     */
    def determineClassImmutability(
        superClassType:                ObjectType,
        superClassInformation:         EOptionP[Entity, Property],
        superClassImmutabilityIsFinal: Boolean,
        lazyComputation:               Boolean
    )(
        cf: ClassFile
    ): ProperPropertyComputationResult = {
        val t = cf.thisType

        var dependees = Map.empty[Entity, EOptionP[Entity, Property]]

        if (!superClassImmutabilityIsFinal) {
            dependees += (SuperClassKey -> superClassInformation)
        }

        // Collect all fields for which we need to determine the effective immutability!
        var hasFieldsWithUnknownImmutability = false

        val instanceFields = cf.fields.iterator.filter { f =>
            !f.isStatic
        }.toList
        var hasNonTransitivelyImmutableFields = false
        var hasDependentlyImmutableFields = false
        var genericTypeParameters: SortedSet[String] = SortedSet.empty

        val fieldsPropertyStoreInformation = propertyStore(instanceFields, FieldImmutability)

        fieldsPropertyStoreInformation.foreach {

            case FinalP(MutableField) =>
                if (lazyComputation)
                    return Result(t, MutableClass);
                else
                    return createResultForAllSubtypes(t, MutableClass);

            case FinalP(NonTransitivelyImmutableField) => hasNonTransitivelyImmutableFields = true

            case FinalP(DependentlyImmutableField(parameters)) =>
                genericTypeParameters ++= parameters
                hasDependentlyImmutableFields = true

            case FinalP(TransitivelyImmutableField) =>

            case ep @ InterimE(e) =>
                hasFieldsWithUnknownImmutability = true
                dependees += (e -> ep)

            case epk @ EPK(e: Entity, _) =>
                // <=> The immutability information is not yet available.
                hasFieldsWithUnknownImmutability = true
                dependees += (e -> epk)

            case _ =>
                if (lazyComputation) // TODO check
                    return Result(t, MutableClass);
                else
                    return createResultForAllSubtypes(t, MutableClass);
        }

        var minLocalImmutability: ClassImmutability = MutableClass

        // NOTE: maxLocalImmutability does not take the super classes' mutability into account!
        var maxLocalImmutability: ClassImmutability = superClassInformation match {
            case UBP(MutableClass)                  => MutableClass
            case UBP(NonTransitivelyImmutableClass) => NonTransitivelyImmutableClass
            case UBP(DependentlyImmutableClass(parameters)) =>
                genericTypeParameters ++= parameters
                DependentlyImmutableClass(genericTypeParameters)
            case _ => TransitivelyImmutableClass
        }
        if (hasNonTransitivelyImmutableFields) {
            maxLocalImmutability = NonTransitivelyImmutableClass
        }

        if (hasDependentlyImmutableFields &&
            maxLocalImmutability != NonTransitivelyImmutableClass && maxLocalImmutability != MutableClass) {
            maxLocalImmutability = DependentlyImmutableClass(genericTypeParameters)
        }

        if (cf.fields.exists(f => !f.isStatic && f.fieldType.isArrayType)) {
            // IMPROVE We could analyze if the array is effectively final.
            // I.e., it is only initialized once (at construction time) and no reference to it
            // is passed to another object.
            maxLocalImmutability = NonTransitivelyImmutableClass
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
                cfImmutabilityIsFinal = true,
                Result(t, maxLocalImmutability)
            );
        }

        def c(someEPS: SomeEPS): ProperPropertyComputationResult = {
            // [DEBUG]
            // val oldDependees = dependees
            dependees = dependees.iterator.filter(_._1 ne someEPS.e).toMap
            someEPS match {
                // Superclass related dependencies:
                //
                case UBP(MutableClass) =>
                    return Result(t, MutableClass);

                case LBP(TransitivelyImmutableClass) => // the super class
                    dependees -= SuperClassKey

                case UBP(NonTransitivelyImmutableClass) => // super class is at most immutable container
                    if (someEPS.isFinal) dependees -= SuperClassKey
                    maxLocalImmutability = NonTransitivelyImmutableClass

                case UBP(DependentlyImmutableClass(parameter)) =>
                    if (someEPS.isFinal) dependees -= SuperClassKey
                    if (maxLocalImmutability != NonTransitivelyImmutableClass) {
                        genericTypeParameters ++= parameter
                        maxLocalImmutability = DependentlyImmutableClass(genericTypeParameters)
                    }

                case LBP(NonTransitivelyImmutableClass) => // super class is at least non-transitively immutable
                    if (minLocalImmutability != NonTransitivelyImmutableClass &&
                        !dependees.valuesIterator.exists(_.pk == FieldImmutability.key))
                        minLocalImmutability = NonTransitivelyImmutableClass // Lift lower bound when possible

                case LUBP(MutableClass, TransitivelyImmutableClass) => // No information about superclass

                case FinalP(DependentlyImmutableField(parameter)) =>
                    if (hasNonTransitivelyImmutableFields) {
                        maxLocalImmutability = NonTransitivelyImmutableClass
                    } else if (maxLocalImmutability != MutableClass &&
                        maxLocalImmutability != NonTransitivelyImmutableClass) {
                        genericTypeParameters ++= parameter
                        maxLocalImmutability = DependentlyImmutableClass(genericTypeParameters)
                    }

                // Field Immutability related dependencies:
                case FinalP(TransitivelyImmutableField) =>
                case FinalP(NonTransitivelyImmutableField) =>
                    maxLocalImmutability = NonTransitivelyImmutableClass
                case FinalP(MutableField) => return Result(t, MutableClass);
                case UBP(MutableField)    => return Result(t, MutableClass);
                case ELBP(e, NonTransitivelyImmutableField |
                    TransitivelyImmutableField) => dependees -= e
                case UBP(TransitivelyImmutableField)    => // no information about field mutability
                case UBP(NonTransitivelyImmutableField) => maxLocalImmutability = NonTransitivelyImmutableClass
                case UBP(DependentlyImmutableField(parameter)) if maxLocalImmutability != NonTransitivelyImmutableClass =>
                    genericTypeParameters ++= parameter
                    maxLocalImmutability =
                        DependentlyImmutableClass(genericTypeParameters)
                case _ => Result(t, MutableClass) // TODO
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

            if (dependees.isEmpty || minLocalImmutability == maxLocalImmutability) {
                Result(t, maxLocalImmutability)
            } else {
                InterimResult(t, minLocalImmutability, maxLocalImmutability, dependees.values.toSet, c)
            }
        }

        val result =
            InterimResult(t, minLocalImmutability, maxLocalImmutability, dependees.values.toSet, c)
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

trait ClassImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(ClassImmutability)

    final override def uses: Set[PropertyBounds] =
        PropertyBounds.lubs(ClassImmutability, FieldImmutability)

    override def requiredProjectInformation: ProjectInformationKeys = Seq()

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
        // java.lang.Object is by definition transitively immutable.
        set(ObjectType.Object, TransitivelyImmutableClass)

        // 1.2
        // All (instances of) interfaces are (by their very definition) also transitively immutable.
        val allInterfaces = project.allClassFiles.filter(cf => cf.isInterfaceDeclaration)
        allInterfaces.foreach(cf => set(cf.thisType, TransitivelyImmutableClass))

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
                    set(cf.thisType, MutableClass)
                }
            }
        }

        // 3.
        // Compute the initial set of classes for which we want to determine the mutability.
        var cfs: List[ClassFile] = Nil
        classHierarchy
            .directSubclassesOf(ObjectType.Object)
            .iterator
            .map(ot => (ot, project.classFile(ot)))
            .foreach {
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
                        set(cf.thisType, MutableClass)
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
 * Scheduler to run the class immutability analysis eagerly.
 * @author Tobias Roth
 * @author Michael Eichberg
 */
object EagerClassImmutabilityAnalysis extends ClassImmutabilityAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, cfs: InitializationData): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(p)
        ps.scheduleEagerComputationsForEntities(cfs)(
            analysis.determineClassImmutability(
                superClassType = null,
                FinalEP(ObjectType.Object, TransitivelyImmutableClass),
                superClassImmutabilityIsFinal = true,
                lazyComputation = false
            )
        )
        analysis
    }
}

/**
 * Scheduler to run the class immutability analysis lazily.
 * @author Michael Eichberg
 */
object LazyClassImmutabilityAnalysis extends ClassImmutabilityAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: InitializationData
    ): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            ClassImmutability.key,
            analysis.doDetermineClassImmutability
        )
        analysis
    }
}
