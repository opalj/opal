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
package br
package analyses
package fp

import org.opalj.fp.Result
import org.opalj.fp.Entity
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyKey
import org.opalj.fp.Property
import org.opalj.fp.PropertyComputationResult
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.fp.ImmediateResult
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.fp.Continuation
import org.opalj.fp.IntermediateResult
import org.opalj.br.Method
import org.opalj.fpa.FixpointAnalysis
import org.opalj.fpa.FilterEntities

sealed trait Instantiability extends Property {
    final def key = Instantiability.Key // All instances have to share the SAME key!
}

object Instantiability {
    final val Key = PropertyKey.create("Instantiability", Instantiable)
}

case object NonInstantiable extends Instantiability { final val isRefineable = true }

case object Instantiable extends Instantiability { final val isRefineable = true }

object InstantiabilityAnalysis
        extends FixpointAnalysis
        with FilterEntities[ClassFile] {

    val propertyKey = Instantiability.Key

    private val factoryPropertyKey = org.opalj.br.analyses.fp.FactoryMethod.Key
    private val isFactoryMethodProperty = org.opalj.br.analyses.fp.IsFactoryMethod

    private val serializableType = ObjectType.Serializable

    private def evaluateFactoryInstantiablity(classFile: ClassFile): Continuation = {
        (dependeeE: Entity, dependeeP: Property) ⇒
            if (dependeeP == isFactoryMethodProperty)
                Result(classFile, Instantiable)
            else Result(classFile, NonInstantiable)
    }

    private def determineInstantiabilityByFactoryMethod(
        classFile: ClassFile,
        method: Method)(
            implicit project: Project[URL],
            propertyStore: PropertyStore): Option[PropertyComputationResult] = {
        import propertyStore.require
        return require(method, factoryPropertyKey,
            classFile, propertyKey)(evaluateFactoryInstantiablity(classFile)) match {
                case res @ Result(_, Instantiable) ⇒ Some(res)
                case _                             ⇒ None
            }
    }

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineProperty(
        classFile: ClassFile)(
            implicit project: Project[URL],
            propertyStore: PropertyStore): PropertyComputationResult = {
        //TODO: check further method visibility according to the computed property. 
        //    -> classes with only non-visible constructors are not instantiable by the client
        import project.classHierarchy.isSubtypeOf

        if (classFile.isAbstract || classFile.isInterfaceDeclaration)
            return Result(classFile, NonInstantiable)

        val declaringType = classFile.thisType

        if (isSubtypeOf(declaringType, serializableType).isYesOrUnknown &&
            classFile.constructors.exists { i ⇒
                i.descriptor.parametersCount == 0
            })
            return Result(classFile, Instantiable)

        val subClassInstantiable = classFile.isFinal && _

        if (classFile.constructors.exists { i ⇒ i.isPublic || subClassInstantiable(i.isProtected) })
            return Result(classFile, Instantiable)

        if (isSubtypeOf(classFile.thisType, ObjectType.Serializable).isYesOrUnknown && classFile.constructors.exists { i ⇒ i.descriptor.parametersCount == 0 })
            return Result(classFile, Instantiable)

        classFile.methods foreach { method ⇒
            if (method.isStatic && !method.isStaticInitializer) {
                val res = determineInstantiabilityByFactoryMethod(classFile, method)
                if (res.isDefined)
                    return res.get
                //                require(method, factoryPropertyKey,
                //                    classFile, propertyKey)(evaluateFactoryInstantiablity(classFile))
            }
        }

        Result(classFile, NonInstantiable)
    }

    val entitySelector: PartialFunction[Entity, ClassFile] = {
        case cf: ClassFile ⇒ cf
    }
}