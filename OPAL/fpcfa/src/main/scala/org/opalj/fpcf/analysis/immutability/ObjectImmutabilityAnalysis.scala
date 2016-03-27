/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package analysis
package immutability

import scala.collection.mutable

import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType

class ObjectImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis {

    final def classHierarchy = project.classHierarchy

    /**
     * @param superClassFile The direct super class (file) of the class file `cf`.
     * 		Can be `null` if `superClassMutability` is `ImmutableObject`.
     */
    def determineObjectImmutability(
        superClassFile:       ClassFile,
        superClassMutability: ObjectImmutability // either immutable or conditionally immutable
    )(
        cf: ClassFile
    ): PropertyComputationResult = {

        /*
         * Create a result object that sets this type and all subclasses of if
         * to the given immutability rating.
         */
        @inline def createMultiResult(immutability: ObjectImmutability) = {
            MultiResult(
                classHierarchy.allSubclassTypes(cf.thisType, reflexive = true).
                map(project.classFile(_)).collect({ case Some(cf) ⇒ cf }).
                map(cf ⇒ EP(cf, immutability)).toTraversable
            )
        }

        if (cf.fields.exists(f ⇒ !f.isFinal && !f.isStatic)) {
            // IMPROVE Test if the field is effectively final.

            // The class is definitively mutable and therefore also all subclasses.
            return createMultiResult(MutableObjectByAnalysis);
        }

        // When we reach this point all instance fields are (effectively) final.

        @inline def directSubclasses(): Traversable[ClassFile] =
            classHierarchy.directSubtypesOf(cf.thisType).view.
                map(ot ⇒ project.classFile(ot)).
                collect { case Some(cf) ⇒ cf }.force

        @inline def createIncrementalResult(
            result:               PropertyComputationResult,
            superClassMutability: ObjectImmutability
        ): IncrementalResult[ClassFile] = {
            IncrementalResult(
                result,
                directSubclasses() map { c ⇒
                    (determineObjectImmutability(cf, superClassMutability) _, c)
                }
            )
        }

        @inline def createConditionallyImmutableIncrementalResult(): IncrementalResult[ClassFile] = {
            createIncrementalResult(
                ImmediateResult(cf, ConditionallyImmutableObject),
                ConditionallyImmutableObject
            );
        }

        if (cf.fields.exists(f ⇒ f.fieldType.isArrayType))
            // IMPROVE We could analyze if the array is effectively final. I.e., it is only initialized once and no reference to it is passed to another object.
            return createConditionallyImmutableIncrementalResult();

        val fieldTypes =
            // IMPROVE Use the precise type of the field (if available)!
            mutable.Set.empty[ObjectType] ++
                cf.fields.collect { case f if f.fieldType.isObjectType ⇒ f.fieldType.asObjectType }

        val dependentClassFiles = mutable.Set.empty[ClassFile]
        val hasUnresolvableDependencies =
            fieldTypes.exists { t ⇒
                project.classFile(t) match {
                    case Some(cf) ⇒
                        dependentClassFiles += cf; false
                    case None ⇒ /* we have an unresolved dependency */ true
                }
            }

        if (hasUnresolvableDependencies)
            return createConditionallyImmutableIncrementalResult;

        // For each dependent class file we now have to determine the mutability
        // of instances of the respective type to determine this type's immutability.
        // Basically, we have to distinguish the following cases:
        // - A field's type is mutable or conditionally immutable=>
        //            This class is Conditionally Immutable.
        // - A field's type is immutable =>
        //            The field is ignored.
        //            (This class is as immutable as its superclass)
        // - A field's type is at least conditionally mutable or
        //   The immutability of the field's type is not yet known =>
        //            This type is AtLeastConditionallyImmutable
        //            We have to declare a dependency on the respective type's immutability.
        //
        // If the supertype is Immutable =>
        //            "nothing special to do"
        // If the supertype is AtLeastConditionallyImmutable =>
        //            "we have to declare a dependency on the supertype"
        val fieldTypesImmutability = propertyStore(dependentClassFiles, TypeImmutability.key)
        val hasMutableOrConditionallyImmutableField =
            // IMPROVE Use the precise type of the field (if available)!
            fieldTypesImmutability.exists { eOptP ⇒
                eOptP.hasProperty && (eOptP.p.isMutable || eOptP.p.isConditionallyImmutable)
            }
        if (hasMutableOrConditionallyImmutableField)
            return createMultiResult(ConditionallyImmutableObject);

        val incompleteDependencies: Traversable[EOptionP[Entity, Property]] =
            fieldTypesImmutability.filterNot { eOptP ⇒
                eOptP.hasProperty && (eOptP.p == ImmutableType)
            }

        if (incompleteDependencies.isEmpty) {
            if (superClassMutability == ImmutableObject) {
                createIncrementalResult(
                    ImmediateResult(cf, superClassMutability),
                    superClassMutability
                )
            } else if (superClassMutability == AtLeastConditionallyImmutableObject) {
                val dependees = Traversable(EP(superClassFile, superClassMutability))
                val c = (e: Entity, p: Property, ut: UserUpdateType) ⇒ {
                    Result(cf, p)
                }
                val result = IntermediateResult(cf, superClassMutability, dependees, c)

                createIncrementalResult(result, superClassMutability)
            } else {
                createIncrementalResult(
                    ImmediateResult(cf, ConditionallyImmutableObject),
                    ConditionallyImmutableObject
                )
            }
        } else {
            // We have dependencies to a type for which the immutability information
            // is not yet complete
            var dependees: Set[EOptionP[Entity, Property]] = incompleteDependencies.toSet[EOptionP[Entity, Property]]
            if (superClassMutability == AtLeastConditionallyImmutableObject)
                dependees = dependees + EP(superClassFile, superClassMutability)

            def c(e: Entity, p: Property, ut: UserUpdateType): PropertyComputationResult = {
                p match {

                    case UnknownTypeImmutability | AtLeastConditionallyImmutableType ⇒
                        // we now have some information, but it doesn't change a thing...
                        dependees = dependees.filter(_.e ne e) + EP(e, p)
                        IntermediateResult(
                            cf, AtLeastConditionallyImmutableObject,
                            dependees, c
                        )

                    case ConditionallyImmutableObject |
                        ConditionallyImmutableType | MutableType ⇒
                        Result(cf, ConditionallyImmutableObject)

                    case ImmutableType | ImmutableObject ⇒
                        if (dependees.size == 1) // we have no other dependencies than the current one
                            Result(cf, ImmutableObject)
                        else {
                            dependees = dependees.filter(_.e ne e)
                            IntermediateResult(cf, AtLeastConditionallyImmutableObject, dependees, c)
                        }
                }
            }

            val result = IntermediateResult(cf, AtLeastConditionallyImmutableObject, dependees, c)
            createIncrementalResult(result, AtLeastConditionallyImmutableObject)
        }

    }

}

/**
 * Runs an immutability analysis to determine the mutability of objects.
 *
 * @author Michael Eichberg
 */
object ObjectImmutabilityAnalysis extends FPCFAnalysisRunner {

    override def recommendations: Set[FPCFAnalysisRunner] = Set.empty

    override def derivedProperties: Set[PropertyKind] = Set(ObjectImmutability)

    override def usedProperties: Set[PropertyKind] = Set(TypeImmutability)

    def start(project: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new ObjectImmutabilityAnalysis(project)
        val classHierarchy = project.classHierarchy

        // 1.1
        // java.lang.Object is by definition immutable
        project.classFile(ObjectType.Object) foreach { cf ⇒
            ps.handleResult(ImmediateResult(cf, ImmutableObject))
        }

        // 1.2
        // all (instances of) interfaces are (by their very definition) also immutable
        ps.handleResult(ImmediateMultiResult(
            project.allClassFiles.
                filter(cf ⇒ cf.isInterfaceDeclaration).
                map(cf ⇒ EP(cf, ImmutableObject))
        ))

        // 2.
        // all classes that do not have complete superclass information are mutable
        // due to the lack of knowledge
        val typesForWhichItMayBePossibleToComputeTheMutability = {
            classHierarchy.allSubtypes(ObjectType.Object, reflexive = true)
        }
        classHierarchy.rootTypes.
            filter(_ ne ObjectType.Object).
            map(rt ⇒ classHierarchy.allSubtypes(rt, reflexive = true)).flatten.
            filter(ot ⇒ !typesForWhichItMayBePossibleToComputeTheMutability.contains(ot)).
            foreach(ot ⇒ project.classFile(ot) foreach { cf ⇒
                ps.handleResult(ImmediateResult(cf, MutableObjectDueToUnknownSupertypes))
            })

        // 3.
        // the initial set of classes for which we want to determine the mutability
        val es = project.classHierarchy.directSubtypesOf(ObjectType.Object).view.
            map(ot ⇒ project.classFile(ot)).
            collect { case Some(cf) if !cf.isInterfaceDeclaration ⇒ cf }

        ps <|<< (es, analysis.determineObjectImmutability(null, ImmutableObject))

        analysis
    }

}
