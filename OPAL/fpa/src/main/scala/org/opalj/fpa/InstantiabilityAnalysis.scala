/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package fpa

import org.opalj.fp.Result
import org.opalj.fp.Entity
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyKey
import org.opalj.fp.Property
import org.opalj.fp.PropertyComputationResult
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.fp.ImmediateResult
import org.opalj.br.Method
import org.opalj.fp.IntermediateResult
import org.opalj.fp.EOptionP
import org.opalj.fp.EP
import org.opalj.fp.EPK
import org.opalj.fp.Unchanged
import org.opalj.fp.Continuation
import org.opalj.br.analyses.SomeProject

sealed trait Instantiability extends Property {
    final def key = Instantiability.Key // All instances have to share the SAME key!
}

object Instantiability {
    final val Key = PropertyKey.create("Instantiability", Instantiable)
}

case object NotInstantiable extends Instantiability { final val isRefineable = false }

case object Instantiable extends Instantiability { final val isRefineable = false }

case object MaybeInstantiable extends Instantiability { final val isRefineable = true }

object InstantiabilityAnalysis
        extends AssumptionBasedFixpointAnalysis
        with FilterEntities[ClassFile] {

    val propertyKey = Instantiability.Key

    private final val FactoryPropertyKey = FactoryMethod.Key
    //    private final val isFactoryMethodProperty = IsFactoryMethod

    private final val SerializableType = ObjectType.Serializable

    // TOOD Method name..?
    private def determineInstantiabilityByFactoryMethod(
        classFile: ClassFile)(
            implicit project: SomeProject,
            propertyStore: PropertyStore): PropertyComputationResult = {

        val methods = classFile.methods.filter(m ⇒ m.isStatic && !m.isStaticInitializer)
        var dependees = Set.empty[EOptionP]

        var i = 0
        while (i < methods.length) {
            val curMethod = methods(i)
            val factoryMethod = propertyStore(curMethod, FactoryPropertyKey)
            factoryMethod match {
                case Some(IsFactoryMethod) ⇒ return ImmediateResult(classFile, Instantiable);
                case _ ⇒
                    assert(factoryMethod.isEmpty || factoryMethod.get == NotFactoryMethod)
                    dependees += EPK(curMethod, FactoryPropertyKey)
                //                case Some(NotFactoryMethod) ⇒ dependees += EPK(curMethod, FactoryPropertyKey)
                //                case None                   ⇒ dependees += EPK(curMethod, FactoryPropertyKey)
                //                case _ ⇒
                //                    val message = s"unknown factory method $factoryMethod"
                //                    throw new UnknownError(message)
            }
            i += 1
        }

        var subTypes = project.classHierarchy.directSubtypesOf(classFile.thisType)
        while (subTypes.nonEmpty) {
            val curSubtype = subTypes.head
            val cf = project.classFile(curSubtype)
            val instantiability = propertyStore(curSubtype, propertyKey)
            instantiability match {
                case Some(Instantiable) ⇒ return ImmediateResult(classFile, Instantiable);
                case Some(MaybeInstantiable) |
                    None ⇒ dependees += EPK(cf, propertyKey)
                case _ ⇒
                    val message = s"unknown instantiability $instantiability"
                    throw new UnknownError(message)
            }
            subTypes ++= project.classHierarchy.directSubtypesOf(curSubtype)
            subTypes -= curSubtype
        }
        // Now: the class is not public has no (yet known) factory method, has no known instantiable subtype,... 
        //if(dependees.isEmpty)... return ;

        val continuation = new Continuation {
            // We use the set of remaining dependencies to test if we have seen
            // all remaining properties.
            var remainingDependendees = dependees.map(eOptionP ⇒ eOptionP.e)

            def apply(e: Entity, p: Property): PropertyComputationResult = this.synchronized {
                if (remainingDependendees.isEmpty)
                    return Unchanged;

                p match {
                    case Instantiable ⇒
                        remainingDependendees = Set.empty
                        Result(classFile, Instantiable)

                    case MaybeInstantiable ⇒ Unchanged

                    case NotInstantiable ⇒
                        remainingDependendees -= e
                        if (remainingDependendees.isEmpty) Result(classFile, NotInstantiable)
                        else Unchanged

                    case IsFactoryMethod ⇒
                        remainingDependendees = Set.empty
                        Result(classFile, Instantiable)

                    case NotFactoryMethod ⇒
                        remainingDependendees -= e
                        if (remainingDependendees.isEmpty) {
                            Result(classFile, NotInstantiable)
                        } else
                            Unchanged
                    case _ ⇒
                        val message = s"unknown property $p"
                        throw new UnknownError(message)
                }
            }
        }

        IntermediateResult(classFile, MaybeInstantiable, dependees, continuation)
    }

    def determineProperty(
        classFile: ClassFile)(
            implicit project: SomeProject,
            propertyStore: PropertyStore): PropertyComputationResult = {
        //TODO: check further method visibility according to the computed property. 
        //    -> classes with only non-visible constructors are not instantiable by the client
        import project.classHierarchy.isSubtypeOf

        if (classFile.isAbstract || classFile.isInterfaceDeclaration)
            return ImmediateResult(classFile, NotInstantiable)

        val classType = classFile.thisType

        if (isSubtypeOf(classType, SerializableType).isYesOrUnknown &&
            classFile.hasDefaultConstructor)
            return ImmediateResult(classFile, Instantiable)

        val nonFinalClass = !classFile.isFinal

        // FIXME Handle just private constructors...
        //        if(nonFinalClass && isPublic && isOpenLibrary...)
        //            ...
        // if(...) {
        //        if(isOpenLibrary)
        //                return;
        //        else...     
        //}
        if (classFile.isPublic && classFile.constructors.exists {
            cons ⇒
                cons.isPublic || (nonFinalClass && (
                    isOpenLibrary || cons.isProtected))
        })
            return ImmediateResult(classFile, Instantiable)

        val instantiability = determineInstantiabilityByFactoryMethod(classFile)
        instantiability
    }

    val entitySelector: PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile ⇒ cf
    }
}