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

import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.fpcf.analysis.fields.FieldUpdates
import org.opalj.fpcf.analysis.fields.FieldUpdatesAnalysis
import org.opalj.fpcf.analysis.fields.EffectivelyFinal
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.FieldAccessInformationKey

/**
 * Analysis the mutability of the instances of a class.
 * An object is immutable if it only has instance fields that are final or effectively final; a
 * subclass of a class is at most as mutable as its superclass – `java.lang.Object` is immutable –
 * a class is conditionally immutable if the instances of the class are not mutable, but the
 * instances may reference mutable fields.
 *
 * @note In Java, technically, no object is immutable because every object is associated with
 * 		a monitor and the state of the monitor changes as soon as we acquire the respective lock
 *      using `synchronized`. I.e., even though an instance of `java.lang.Object` is typically
 *      regarded as immutable the monitor's state can be changed using `synchronized`. However,
 *      for this analysis the state regarded to the object's implicit monitor is not considered.
 */
class ImmutabilityAnalysis(
        val project:                SomeProject,
        val fieldAccessInformation: FieldAccessInformation
) extends FPCFAnalysis {

    val classHierarchy = project.classHierarchy

    import classHierarchy.{directSubtypesOf ⇒ subtypes}
    import propertyStore.require

    private def thisProjectClassTypes(objectTypes: Traversable[ObjectType]): Traversable[ClassFile] = {
        objectTypes.view.
            map(objectType ⇒ project.classFile(objectType)).
            collect { case Some(classFile) if !classFile.isInterfaceDeclaration ⇒ classFile }
    }

    private def allProjectSubclasses(supertype: ObjectType): Traversable[ClassFile] = {
        val allSubclasses = classHierarchy.allSubclasses(supertype, reflexive = true).toList
        thisProjectClassTypes(allSubclasses)
    }

    // implements the IncrementalPropertyComputation
    abstract class ImmutabilityComputation(
            val superclassMutability: ObjectImmutability
    ) extends (Entity ⇒ IncrementalPropertyComputationResult) {

        def apply(classFileEntity: Entity): IncrementalPropertyComputationResult = {
            //            val classFile = classFileEntity.asInstanceOf[ClassFile]
            //            val supertype = classFile.thisType
            //            
            //            var currentImmutability = superclassMutability
            //            val instanceFields = classFile.fields.filter{field => 
            //                !field.isStatic && !(
            //                        // The field is effectively final and has a primitive type. The latter
            //                        // is required because it ensures that we have no effect on the current 
            //                        // immutability.
            //                      field.fieldType.isBaseType &&
            //                (field.isPrivate /* IMPROVE => we know all accesses */)&&
            //                fieldAccessInformation.writeAccesses(classFile,field).forall{ writeAccess =>
            //                   val (method,_/*PCs*/) =  writeAccess
            //                   method.isConstructor /* IMPROVE => only written at initialization time */
            //                }    
            //                )}.toList
            //
            //            if (instanceFields.isEmpty /*i.e., we have no (relevant) instance fields */) {
            //                return IncrementalPropertyComputationResult(
            //                    ImmediateResult(classFile, currentImmutability),
            //                    thisProjectClassTypes(subtypes(supertype)).map(cf ⇒ (this, cf))
            //                );
            //            }
            //            
            //            // let's narrow down the list of fields about which we need further information..
            //            val referenceInstanceFields = instanceFields.filter { field =>
            //               
            //            }

            //
            //            // We have to check for each field whether it is effectively final. 
            //            // if so, we have to check if the field type's immutability to determine
            //            // whether this class is Immutable or just ConditionallyImmutable.
            //            
            //            def analysis(e: Entity, p: Property) = analysis(p)
            //
            //            def analysis(p: Property): PropertyComputationResult = {
            //                
            //                val c : Continuation = { (e, p) => p match { 
            //                    case EffectivelyFinal | Final  =>
            //                        
            //                    case _ /* not final... */  =>
            //                        
            //                }}
            //                    val instanceField = instanceFields.head
            //                    instanceFields = instanceFields.tail
            //                    require(classFile, Immutability.key, instanceField, Mutated.key) { (e, p) ⇒
            //
            //                    }
            //            }
            ???
        }
    }

    object ImmutableSuperclassComputation extends ImmutabilityComputation(ImmutableObject)
    object AtLeastConditionallyImmutableSuperclassComputation extends ImmutabilityComputation(AtLeastConditionallyImmutableObject)
    object ConditionallyImmutableSuperclassComputation extends ImmutabilityComputation(ConditionallyImmutableObject)

    def determineInitialProperty(
        classFileEntity: Entity
    ): IncrementalPropertyComputationResult = {
        val classFile = classFileEntity.asInstanceOf[ClassFile]
        val thisType = classFile.thisType

        if (thisType eq ObjectType.Object)
            IncrementalPropertyComputationResult(
                ImmediateResult(classFile, ImmutableObject),
                thisProjectClassTypes(subtypes(thisType)).map(cf ⇒ (ImmutableSuperclassComputation, cf))
            )
        else {
            // We don't know anything about thisType's super types, hence, we 
            // we make the conservative assumption that this type is mutable.
            IncrementalPropertyComputationResult(
                ImmediateMultiResult(allProjectSubclasses(thisType).map { subclass ⇒
                    (subclass, MutableObjectDueToUnknownSupertypes)
                }),
                Nil
            )
        }
    }

}

object ImmutabilityAnalysisRunner extends FPCFAnalysisRunner {

    override def recommendations: Set[FPCFAnalysisRunner] = Set(FieldUpdatesAnalysis)

    override def derivedProperties: Set[PropertyKind] = Set(ObjectImmutability)

    override def usedProperties: Set[PropertyKind] = Set(FieldUpdates)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        import project.classHierarchy.rootTypes
        val fieldAccessInformation = project.get(FieldAccessInformationKey)
        val analysis = new ImmutabilityAnalysis(
            project,
            fieldAccessInformation
        )
        propertyStore <^< (
            rootTypes.map(objectType ⇒ project.classFile(objectType).get),
            analysis.determineInitialProperty _
        )
        analysis
    }

}

/*

/**
 * This analysis determines which classes in a project are immutable,
 * conditionally immutable or mutable. If this analysis cannot finally assess
 * the mutability of a class, the result will be unknown. In general, the analysis
 * will always only assess a class as conditionally immutable or immutable if the
 * analysis can guarantee the property to always hold.
 *
 * @author Andre Pacak
 * @author Michael Eichberg
 */
object ImmutabilityAnalysis {

    private def fieldBasedRating(
        classFile:            ClassFile,
        superclassTypeRating: MutabilityRating
    ): MutabilityRating = {

        assert(!(classFile.thisType eq ObjectType.Object))
        assert(!classFile.isInterfaceDeclaration)
        assert(superclassTypeRating.id > Mutable.id)

        var rating = superclassTypeRating

        classFile.fields.foreach { field ⇒
            if (field.isFinal) {
                if (field.fieldType.isReferenceType)
                    rating = ConditionallyImmutable
                // else
                //  (field.isBaseType === true) => nothing to do
            } else {
                return Unknown;
            }
        }

        rating
    }

    private def methodBasedRating(
        classFile:            ClassFile,
        superclassTypeRating: MutabilityRating
    ): MutabilityRating = {

        assert(!(classFile.thisType eq ObjectType.Object))
        assert(!classFile.isInterfaceDeclaration)
        assert(superclassTypeRating.id > Mutable.id)

        val definesSimpleSetterMethod = {
            val concreteMethods =
                classFile.methods.view.filter { method ⇒
                    !method.isConstructor && method.body.nonEmpty && !method.isPrivate
                }
            concreteMethods exists { method ⇒
                method.body.get.matchTriple(
                    1,
                    { (instr1: Instruction, instr2: Instruction, instr3: Instruction) ⇒
                        (instr1, instr2, instr3) match {
                            case (
                                ALOAD_0,
                                LoadLocalVariableInstruction(_, 1),
                                PUTFIELD(objectType, _, _)
                                ) ⇒ true
                            case _ ⇒ false
                        }
                    }
                ).nonEmpty
            }
        }
        if (definesSimpleSetterMethod)
            Mutable
        else
            Unknown
    }
    /**
     * Rates the mutability of all class files of the project.
     *
     * @param project The project that we are analyzing.
     * @param isInterrupted A function that can interrupt the algorithm from the outside.
     * @return A map with mutability ratings for class types.
     */
    def doAnalyze(
        project:       Project[URL],
        isInterrupted: () ⇒ Boolean = () ⇒ false
    ): Map[ObjectType, MutabilityRating] = {
        val classHierarchy = project.classHierarchy
        import classHierarchy.foreachDirectSubclass

        val classification = ConcurrentMap.empty[ObjectType, MutabilityRating]
        classification(ObjectType.Object) = Immutable // initial configuration

        def traverse(classFile: ClassFile, superclassTypeRating: MutabilityRating): Unit = {
            if (isInterrupted())
                return ;

            val classType = classFile.thisType
            fieldBasedRating(classFile, superclassTypeRating) match {
                case r @ (Immutable | ConditionallyImmutable) ⇒
                    classification(classType) = r
                    foreachDirectSubclass(classType, project) { subclass ⇒
                        traverse(subclass, superclassTypeRating)
                    }
                case r @ Unknown ⇒
                    val r = methodBasedRating(classFile, superclassTypeRating)
                    classification(classType) = r
                    classHierarchy.foreachSubtype(classType) { subclassType ⇒
                        classification(subclassType) = r
                    }

                case r @ Mutable ⇒
                    classification(classType) = r
                    classHierarchy.foreachSubtype(classType) { subclassType ⇒
                        classification(subclassType) = r
                    }

            }
        }
        // 1. Do the basic classification
        // "java.lang.Object" is at the root of the class hierarchy and we can only
        // assess classes for which all super class type information exists.
        foreachDirectSubclass(ObjectType.Object, project) { subclass ⇒
            traverse(subclass, Immutable)
        }

        // 2. (Re-)Analyze all class files marked as ConditionallyImmutable to
        // check if the transitive hull only contains ConditionallyImmutable or Immutable
        // classes. If so, the class can be reranked to Immutable.

        // TODO... Fixpoint computation

        // we are done...
        classification
    }
}
*/ 