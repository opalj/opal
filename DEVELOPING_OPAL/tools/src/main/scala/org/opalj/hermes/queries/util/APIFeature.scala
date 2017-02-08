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
 * Represents a call to a specific API method.
 *
 * @author Michael Reif
 */
case class APIMethod(
        val declaringClass: ObjectType,
        val name:           String,
        val descriptor:     MethodDescriptor,
        val isStatic:       Boolean          = false
) extends APIFeature {

    /**
     * Returns the feature id which is put together from the declaring class, the method's name and
     * the method's descriptor.
     */
    def toFeatureID: String = {
        s"${declaringClass.fqn}\n${descriptor.toJava(name)}"
    }

    override def getAPIMethods: Chain[APIMethod] = Chain(this)
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
