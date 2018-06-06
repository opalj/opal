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
package hermes
package queries
package util

import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught
import org.opalj.br.instructions.MethodInvocationInstruction

/**
 * A common super trait for API related features such as the usage of common or interesting APIs.
 *
 * @author Michael Reif
 */
sealed abstract class APIFeature {

    /**
     * Returns the feature id of the feature.
     *
     * @note Feature ids have to be unique.
     */
    def featureID: String

    /**
     * Returns all methods of the API that belong to this feature.
     */
    def apiMethods: Chain[APIMethod]
}

/**
 * Common trait that abstracts over all Class extension scenarios.
 *
 */
sealed abstract class ClassExtension extends APIFeature {

    def declClass: ObjectType

    override def apiMethods: Chain[APIMethod] = Naught
}

/**
 * Represents an extension of a specific class
 */
case class APIClassExtension(featureID: String, declClass: ObjectType) extends ClassExtension

/**
 * Common trait that abstracts over instance and static api methods.
 */
sealed abstract class APIMethod(private val fID: Option[String] = None) extends APIFeature {

    def declClass: ObjectType

    def name: String

    def descriptor: Option[MethodDescriptor]

    def unapply(i: MethodInvocationInstruction): Boolean

    final def matches(i: MethodInvocationInstruction): Boolean = this.unapply(i)

    final override val apiMethods = Chain(this)

    private def customFeatureID: Option[String] = fID

    /**
     * Return the feature id of the feature.
     *
     * @note Feature ids have to be unique.
     */
    override val featureID: String = {
        if (customFeatureID.isDefined) {
            customFeatureID.get
        } else {
            val methodName = descriptor.map(_.toJava(name)).getOrElse(name)
            val abbreviatedMethodName = methodName.replaceAll("java.lang.Object", "Object")
            s"${declClass.toJava}\n$abbreviatedMethodName"
        }
    }
}

/**
 * Represents an instance API call.
 *
 * @param  declClass ObjectType of the receiver.
 * @param  name Name of the API method.
 * @param  descriptor Optional method descriptor, is no descriptor assigned, it represents
 *         all methods with the same name, declared in the same class.
 */
case class InstanceAPIMethod(
        declClass:  ObjectType,
        name:       String,
        descriptor: Option[MethodDescriptor],
        fID:        Option[String]           = None
) extends APIMethod(fID) {

    def unapply(i: MethodInvocationInstruction): Boolean = {
        i.isInstanceMethod &&
            this.declClass == i.declaringClass &&
            this.name == i.name &&
            (this.descriptor.isEmpty || this.descriptor.get == i.methodDescriptor)
    }
}

/**
 * Factory for InstanceMethods.
 */
object InstanceAPIMethod {

    def apply(
         declClass: ObjectType,
         name:      String,
         ): InstanceAPIMethod = {
        InstanceAPIMethod(declClass, name, None)
    }

    def apply(
        declClass: ObjectType,
        name:      String,
        featureID: String
    ): InstanceAPIMethod = {
        InstanceAPIMethod(declClass, name, None, Some(featureID))
    }

    def apply(
        declClass:  ObjectType,
        name:       String,
        descriptor: MethodDescriptor
    ): InstanceAPIMethod = {
        InstanceAPIMethod(declClass, name, Some(descriptor))
    }
}

/**
 * Represents a static API call.
 *
 *
 * @param  declClass ObjectType of the receiver.
 * @param  name Name of the API method.
 * @param  descriptor Optional method descriptor, is no descriptor assigned, it represents
 *         all methods with the same name, declared in the same class.
 */
case class StaticAPIMethod(
        declClass:  ObjectType,
        name:       String,
        descriptor: Option[MethodDescriptor],
        fID:        Option[String]           = None
) extends APIMethod(fID) {

    def unapply(i: MethodInvocationInstruction): Boolean = {
        !i.isInstanceMethod &&
            this.declClass == i.declaringClass &&
            this.name == i.name &&
            (this.descriptor.isEmpty || this.descriptor.get == i.methodDescriptor)
    }
}

/**
 * Factory for InstanceMethods.
 */
object StaticAPIMethod {

    def apply(declClass: ObjectType, name: String): StaticAPIMethod = {
        StaticAPIMethod(declClass, name, None)
    }

    def apply(declClass: ObjectType, name: String, featureID: String): StaticAPIMethod = {
        StaticAPIMethod(declClass, name, None, Some(featureID))
    }

    def apply(
        declClass:  ObjectType,
        name:       String,
        descriptor: MethodDescriptor
    ): StaticAPIMethod = {
        StaticAPIMethod(declClass, name, Some(descriptor))
    }
}

/**
 * Represents a collection of API methods that can be mapped to a single feature. Most APIs provide
 * multiple or slightly different API methods to achieve a single task, hence, it can be helpful to
 * group those methods.
 *
 * @note It is assumed that the passed featureID is unique throughout all feature extractors.
 */
case class APIFeatureGroup(apiMethods: Chain[APIMethod], featureID: String) extends APIFeature
