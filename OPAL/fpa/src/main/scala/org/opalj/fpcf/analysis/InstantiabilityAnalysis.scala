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
package fpcf
package analysis

import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.Method
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
    private final val SerializableType = ObjectType.Serializable

    private def instantiableThroughFactoryOrSubclass(
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
                case Some(IsFactoryMethod)  ⇒ return ImmediateResult(classFile, Instantiable);
                case Some(NotFactoryMethod) ⇒ /* Do nothing */
                case _ ⇒
                    assert(factoryMethod.isEmpty)
                    dependees += EPK(curMethod, FactoryPropertyKey)
            }
            i += 1
        }

        var subtypes = project.classHierarchy.directSubtypesOf(classFile.thisType)
        while (subtypes.nonEmpty) {
            val subtype = subtypes.head
            val subclass = project.classFile(subtype).get
            val instantiability = propertyStore(subclass, propertyKey)
            instantiability match {
                case Some(Instantiable)    ⇒ return ImmediateResult(classFile, Instantiable);
                case Some(NotInstantiable) ⇒ /* Do nothing */
                case _ ⇒
                    assert(instantiability.isEmpty || instantiability.get == MaybeInstantiable)
                    dependees += EPK(subclass, propertyKey)
            }
            subtypes ++= project.classHierarchy.directSubtypesOf(subtype)
            subtypes -= subtype
        }
        // Now: the class is not public has no (yet known) factory method, has no known instantiable subtype,... 
        // If the class has no dependees, we know that it is not instantiable
        if (dependees.isEmpty)
            return ImmediateResult(classFile, NotInstantiable)

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

        if (classFile.isPublic)
            classFile.constructors foreach { cons ⇒
                if (cons.isPublic)
                    return ImmediateResult(classFile, Instantiable)
                else if (nonFinalClass &&
                    ((cons.isPackagePrivate && isOpenLibrary) || cons.isProtected))
                    return ImmediateResult(classFile, Instantiable)
            }

        // NOW: 
        //  - the type is neither abstract nor an interface declaration
        //  - the class does not inherit from Serializable
        //  - the class has no globally visible constructors

        val instantiability = instantiableThroughFactoryOrSubclass(classFile)
        instantiability
    }

    val entitySelector: PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile ⇒ cf
    }
}