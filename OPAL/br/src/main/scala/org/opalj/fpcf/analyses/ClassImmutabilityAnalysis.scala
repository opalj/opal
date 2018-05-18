/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.FinalField
import org.opalj.fpcf.properties.ImmutableContainer
import org.opalj.fpcf.properties.ImmutableContainerType
import org.opalj.fpcf.properties.ImmutableObject
import org.opalj.fpcf.properties.ImmutableType
import org.opalj.fpcf.properties.MutableObject
import org.opalj.fpcf.properties.MutableObjectByAnalysis
import org.opalj.fpcf.properties.MutableObjectDueToUnknownSupertypes
import org.opalj.fpcf.properties.MutableType
import org.opalj.fpcf.properties.NonFinalField
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * Determines the mutability of instances of a specific class. In case the class
 * is abstract the (implicit) assumption is made that all abstract methods (if any) are/can
 * be implemented without necessarily/always requiring additional state; i.e., only the currently
 * defined fields are taken into consideration. An interfaces is always considered to be immutable.
 * If you need to know if all possible instances of an interface or some type; i.e., all instances
 * of the classes that implement the respective interface/inherit from some class are immutable,
 * you can query the [[org.opalj.fpcf.properties.TypeImmutability]] property.
 *
 * In case of incomplete class hierarchies or if the class hierarchy is complete, but some
 * class files are not found the sound approximation is done that the respective classes are
 * mutable.
 *
 * This analysis uses the [[org.opalj.fpcf.properties.FieldMutability]] property to determine
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
        val r = allSubtypes.map { st ⇒ new FinalEP(st, immutability) }.toSeq
        MultiResult(r)
    }

    @inline private[this] def createIncrementalResult(
        t:                   ObjectType,
        cfMutability:        EOptionP[Entity, Property],
        cfMutabilityIsFinal: Boolean,
        result:              PropertyComputationResult
    ): IncrementalResult[ClassFile] = {
        var results: List[PropertyComputationResult] = List(result)
        var nextComputations: List[(PropertyComputation[ClassFile], ClassFile)] = Nil
        val directSubtypes = classHierarchy.directSubtypesOf(t)
        directSubtypes.foreach { t ⇒
            project.classFile(t) match {
                case Some(scf) ⇒
                    nextComputations ::= (
                        (determineClassImmutability(t, cfMutability, cfMutabilityIsFinal, false) _, scf)
                    )
                case None ⇒
                    OPALLogger.warn(
                        "project configuration - object immutability analysis",
                        s"missing class file of ${t.toJava}; setting all subtypes to mutable"
                    )
                    results ::= createResultForAllSubtypes(t, MutableObjectDueToUnknownSupertypes)
            }
        }
        IncrementalResult(Results(results), nextComputations)
    }

    def doDetermineClassImmutability(e: Entity): PropertyComputationResult = {
        e match {
            case t: ObjectType ⇒
                //this is safe
                classHierarchy.superclassType(t) match {
                    case None ⇒ Result(t, MutableObjectDueToUnknownSupertypes)
                    case Some(superClassType) ⇒
                        val cf = project.classFile(t) match {
                            case None ⇒
                                return Result(t, MutableObjectByAnalysis) //TODO consider other lattice element
                            case Some(cf) ⇒ cf
                        }

                        propertyStore(superClassType, ClassImmutability.key) match {
                            case EPS(_, _, p: MutableObject) ⇒ Result(t, p)
                            case eps: EPS[ObjectType, ClassImmutability] ⇒
                                determineClassImmutability(
                                    superClassType,
                                    eps,
                                    eps.isFinal,
                                    lazyComputation = true
                                )(cf)
                            case epk ⇒
                                determineClassImmutability(
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
    def determineClassImmutability(
        superClassType:              ObjectType,
        superClassInformation:       EOptionP[Entity, Property],
        superClassMutabilityIsFinal: Boolean,
        lazyComputation:             Boolean
    )(
        cf: ClassFile
    ): PropertyComputationResult = {
        // assert(superClassMutability.isMutable.isNoOrUnknown)
        val t = cf.thisType

        var dependees = Map.empty[Entity, EOptionP[Entity, Property]]

        if (!superClassMutabilityIsFinal) {
            dependees += (SuperClassKey → superClassInformation)
        }

        // Collect all fields for which we need to determine the effective mutability!
        var hasFieldsWithUnknownMutability = false

        val instanceFields = cf.fields.filter { f ⇒ !f.isStatic }
        dependees ++= (propertyStore(instanceFields, FieldMutability) collect {
            case FinalEP(_, _: NonFinalField) ⇒
                // <=> The class is definitively mutable and therefore also all subclasses.
                if (lazyComputation)
                    return Result(t, MutableObjectByAnalysis);
                else
                    return createResultForAllSubtypes(t, MutableObjectByAnalysis);
            case ep @ IntermediateEP(e, _, _) ⇒
                hasFieldsWithUnknownMutability = true
                (e, ep)
            case epk @ EPK(e: Entity, _) ⇒
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
            case EPS(_, _, ImmutableContainer) ⇒ ImmutableContainer
            case _                             ⇒ ImmutableObject
        }

        if (cf.fields.exists(f ⇒ !f.isStatic && f.fieldType.isArrayType)) {
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
                    case f if !f.isStatic && f.fieldType.isObjectType ⇒ f.fieldType.asObjectType
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
            fieldTypesImmutability.exists { eOptP ⇒
                eOptP.hasProperty && (eOptP.ub.isMutable || eOptP.ub.isImmutableContainer)
            }

        if (hasMutableOrConditionallyImmutableField) {
            maxLocalImmutability = ImmutableContainer
        } else {
            val fieldTypesWithUndecidedMutability: Traversable[EOptionP[Entity, Property]] =
                // Recall: we don't have fields which are mutable or conditionally immutable
                fieldTypesImmutability.filterNot { eOptP ⇒
                    eOptP.hasProperty && eOptP.ub == ImmutableType && eOptP.isFinal
                }
            fieldTypesWithUndecidedMutability.foreach { eOptP ⇒
                dependees += (eOptP.e → eOptP)
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

        def c(someEPS: SomeEPS): PropertyComputationResult = {
            //[DEBUG]             val oldDependees = dependees
            someEPS match {
                // Superclass related dependencies:
                //
                case EPS(_, _, _: MutableObject) ⇒ return Result(t, MutableObjectByAnalysis);

                case EPS(_, ImmutableObject, _) ⇒ // the super class
                    dependees -= SuperClassKey

                case EPS(_, _, ImmutableContainer) ⇒ // super class is at most immutable container
                    if (someEPS.isFinal) dependees -= SuperClassKey
                    maxLocalImmutability = ImmutableContainer
                    dependees = dependees.filterNot(_._2.pk == TypeImmutability.key)

                case EPS(_, ImmutableContainer, _) ⇒ // super class is a least immutable container
                    if (minLocalImmutability != ImmutableContainer &&
                        !dependees.valuesIterator.exists(_.pk == FieldMutability.key))
                        minLocalImmutability = ImmutableContainer // Lift lower bound when possible

                case EPS(_, _: MutableObject, ImmutableObject) ⇒ // No information about superclass

                // Properties related to the type of the class's fields.
                //
                case EPS(_, _, ImmutableContainerType | MutableType) ⇒
                    maxLocalImmutability = ImmutableContainer
                    dependees = dependees.filterNot(_._2.pk == TypeImmutability.key)

                case EPS(e, ImmutableType, _) ⇒ // Immutable field type, no influence on mutability
                    dependees -= e

                case EPS(_, _, ImmutableType)    ⇒ // No information about field type

                // Field Mutability related dependencies:
                //
                case EPS(_, _, _: NonFinalField) ⇒ return Result(t, MutableObjectByAnalysis);

                case EPS(e, _: FinalField, _) ⇒
                    dependees -= e
                    if (minLocalImmutability != ImmutableContainer &&
                        !dependees.valuesIterator.exists(_.pk != TypeImmutability.key))
                        minLocalImmutability = ImmutableContainer // Lift lower bound when possible

                case EPS(_, _, _: FinalField) ⇒ // no information about field mutability

            }

            if (someEPS.isRefinable) {
                val entity = if (someEPS.pk == ClassImmutability.key) SuperClassKey else someEPS.e
                dependees += (entity → someEPS)
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
                IntermediateResult(
                    t, minLocalImmutability, maxLocalImmutability, dependees.values, c
                )

            }
        }

        //[DEBUG] assert(initialImmutability.isRefinable)
        val result = IntermediateResult(
            t, minLocalImmutability, maxLocalImmutability, dependees.values, c
        )
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

trait ClassImmutabilityAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(ClassImmutability)

    override def uses: Set[PropertyKind] = Set(TypeImmutability, FieldMutability)

    def setResultsAnComputeEntities(
        project: SomeProject, propertyStore: PropertyStore
    ): TraversableOnce[ClassFile] = {
        val classHierarchy = project.classHierarchy
        import classHierarchy.allSubtypes
        import classHierarchy.rootClassTypes
        import propertyStore.handleResult
        implicit val logContext: LogContext = project.logContext

        // 1.1
        // java.lang.Object is by definition immutable.
        handleResult(Result(ObjectType.Object, ImmutableObject))

        // 1.2
        // All (instances of) interfaces are (by their very definition) also immutable.
        val allInterfaces = project.allClassFiles.filter(cf ⇒ cf.isInterfaceDeclaration)
        handleResult(MultiResult(allInterfaces.map(cf ⇒ new FinalEP(cf.thisType, ImmutableObject))))

        // 2.
        // All classes that do not have complete superclass information are mutable
        // due to the lack of knowledge.
        // But, for classes that directly inherit from Object, but which also
        // implement unknown interface types it is possible to compute the class
        // immutability
        val unexpectedRootClassTypes = rootClassTypes.filter(rt ⇒ rt ne ObjectType.Object)

        unexpectedRootClassTypes foreach { rt ⇒
            allSubtypes(rt, reflexive = true) foreach { ot ⇒
                project.classFile(ot) foreach { cf ⇒
                    handleResult(Result(cf.thisType, MutableObjectDueToUnknownSupertypes))
                }
            }
        }

        // 3.
        // Compute the initial set of classes for which we want to determine the mutability.
        var cfs: List[ClassFile] = Nil
        classHierarchy.directSubclassesOf(ObjectType.Object).toIterator.
            map(ot ⇒ (ot, project.classFile(ot))).
            foreach {
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
                        handleResult(Result(cf.thisType, MutableObjectDueToUnknownSupertypes))
                    })
            }
        cfs
    }
}

/**
 * Scheduler to run the immutability analysis eagerly.
 *
 * @author Michael Eichberg
 */
object EagerClassImmutabilityAnalysis
        extends ClassImmutabilityAnalysisScheduler
        with FPCFEagerAnalysisScheduler {

    override def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {

        val analysis = new ClassImmutabilityAnalysis(project)

        val cfs = setResultsAnComputeEntities(project, propertyStore)
        propertyStore.scheduleEagerComputationsForEntities(cfs)(
            analysis.determineClassImmutability(
                null, FinalEP(ObjectType.Object, ImmutableObject), true, false
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

    override def startLazily(
        project: SomeProject, propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(project)

        setResultsAnComputeEntities(project, propertyStore)
        propertyStore.registerLazyPropertyComputation(
            ClassImmutability.key, analysis.doDetermineClassImmutability
        )
        analysis
    }
}
