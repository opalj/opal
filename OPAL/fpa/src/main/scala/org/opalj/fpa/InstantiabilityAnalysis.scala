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

case object NonInstantiable extends Instantiability { final val isRefineable = false }

case object Instantiable extends Instantiability { final val isRefineable = false }

case object MaybeInstantiable extends Instantiability { final val isRefineable = true }

object InstantiabilityAnalysis
        extends FixpointAnalysis
        with FilterEntities[ClassFile] {

    val propertyKey = Instantiability.Key

    private final val factoryPropertyKey = FactoryMethod.Key
    //    private final val isFactoryMethodProperty = IsFactoryMethod

    private final val serializableType = ObjectType.Serializable

    private def determineInstantiabilityByFactoryMethod(
        classFile: ClassFile)(
            implicit project: SomeProject,
            propertyStore: PropertyStore): PropertyComputationResult = {

        val methods = classFile.methods.filter(m ⇒ m.isStatic && !m.isStaticInitializer)
        var dependees = Set.empty[EOptionP]

        var i = 0
        while (i < methods.length) {
            val curMethod = methods(i)
            val instantiability = propertyStore(curMethod, factoryPropertyKey)
            instantiability match {
                case Some(IsFactoryMethod)  ⇒ return ImmediateResult(classFile, Instantiable)
                case Some(NonFactoryMethod) ⇒ dependees += EPK(curMethod, factoryPropertyKey)
                case None                   ⇒ dependees += EPK(curMethod, factoryPropertyKey)
                case _ ⇒
                    val message = s"unknown instantiability $instantiability"
                    throw new UnknownError(message)
            }
            i += 1
        }

        val continuation = new Continuation {
            // We use the set of remaining dependencies to test if we have seen
            // all remaining properties.
            var remainingDependendees = dependees.map(eOptionP ⇒ eOptionP.e)

            def apply(e: Entity, p: Property): PropertyComputationResult = this.synchronized {
                if (remainingDependendees.isEmpty)
                    return Unchanged;

                p match {
                    case IsFactoryMethod ⇒
                        remainingDependendees = Set.empty
                        Result(classFile, Instantiable)

                    case NonFactoryMethod ⇒
                        remainingDependendees -= e
                        if (remainingDependendees.isEmpty) {
                            Result(classFile, NonInstantiable)
                        } else
                            Unchanged
                }
            }
        }

        IntermediateResult(classFile, MaybeInstantiable, dependees, continuation)
    }

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineProperty(
        classFile: ClassFile)(
            implicit project: SomeProject,
            propertyStore: PropertyStore): PropertyComputationResult = {
        //TODO: check further method visibility according to the computed property. 
        //    -> classes with only non-visible constructors are not instantiable by the client
        import project.classHierarchy.isSubtypeOf

        if (classFile.isAbstract || classFile.isInterfaceDeclaration)
            return ImmediateResult(classFile, NonInstantiable)

        val declaringType = classFile.thisType

        if (isSubtypeOf(declaringType, serializableType).isYesOrUnknown &&
            classFile.hasDefaultConstructor)
            return ImmediateResult(classFile, Instantiable)

        // TODO FIXME ....
        if (classFile.constructors.exists { c ⇒ c.isPublic || (!classFile.isFinal && c.isProtected) })
            return ImmediateResult(classFile, Instantiable)

        val instantiability = determineInstantiabilityByFactoryMethod(classFile)
        instantiability
    }

    val entitySelector: PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile ⇒ cf
    }
}