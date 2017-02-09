/*
 * BSD 2-Clause License:
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

/**
 * A common super trait for API related feature such as the usage of common or interesting APIs.
 *
 * @author Michael Reif
 */
sealed trait APIFeature {

    /**
     * Return the feature id of the the feature.
     *
     * @note Feature ids have to be unique.
     */
    def toFeatureID: String

    /**
     * Returns all methods of the API that belong to this feature.
     */
    def getAPIMethods: Chain[APIMethod]
}

/**
 * Common trait that abstracts over instance and static api methods
 */
sealed trait APIMethod extends APIFeature

/**
 * Represents an instance API call.
 *
 *
 * @param declClass ObjectType of the receiver.
 * @param name Name of the API method.
 * @param descriptor Optional method descriptor, is no descriptor assigned, it represents all methods
 *                   with the same name, declared in the same class.
 */
case class InstanceAPIMethod(
        val declClass:  ObjectType,
        val name:       String,
        val descriptor: Option[MethodDescriptor]
) extends APIMethod {

    /**
     * Return the feature id of the the feature.
     *
     * @note Feature ids have to be unique.
     */
    override def toFeatureID: String = {
        val methodName = descriptor match {
            case Some(md) ⇒ md.toJava(name)
            case None     ⇒ ""
        }

        s"${declClass.fqn}\n$methodName}"
    }
    override def getAPIMethods = Chain(this)
}

/**
 * Factory for InstanceMethods.
 */
object InstanceAPIMethod {

    def apply(declClass: ObjectType, name: String): InstanceAPIMethod = {
        InstanceAPIMethod(declClass, name, None)
    }

    def apply(declClass: ObjectType, name: String, descriptor: MethodDescriptor): InstanceAPIMethod = {
        InstanceAPIMethod(declClass, name, Some(descriptor))
    }
}

/**
 * Represents a static API call.
 *
 *
 * @param declClass ObjectType of the receiver.
 * @param name Name of the API method.
 * @param descriptor Optional method descriptor, is no descriptor assigned, it represents all methods
 *                   with the same name, declared in the same class.
 */
case class StaticAPIMethod(
        val declClass:  ObjectType,
        val name:       String,
        val descriptor: Option[MethodDescriptor]
) extends APIMethod {

    /**
     * Return the feature id of the the feature.
     *
     * @note Feature ids have to be unique.
     */
    override def toFeatureID: String = {
        val methodName = descriptor match {
            case Some(md) ⇒ md.toJava(name)
            case None     ⇒ ""
        }

        s"${declClass.fqn}\n$methodName}"
    }

    override def getAPIMethods = Chain(this)
}

/**
 * Factory for InstanceMethods.
 */
object StaticAPIMethod {

    def apply(declClass: ObjectType, name: String): StaticAPIMethod = {
        StaticAPIMethod(declClass, name, None)
    }

    def apply(declClass: ObjectType, name: String, descriptor: MethodDescriptor): StaticAPIMethod = {
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
case class APIFeatureGroup(
        val apiMethods: Chain[APIMethod],
        val featureID:  String
) extends APIFeature {

    override def toFeatureID: String = this.featureID

    override def getAPIMethods: Chain[APIMethod] = apiMethods
}
