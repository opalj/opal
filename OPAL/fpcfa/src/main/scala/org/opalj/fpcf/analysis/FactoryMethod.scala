/* BSD 2Clause License:
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
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED T
 * PURPOSE
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

/**
 * Common super trait of all factory method properties.
 */
sealed trait FactoryMethod extends Property {

    final def key: org.opalj.fpcf.PropertyKey = FactoryMethod.key

}
/**
 * A companion object for the ProjectAccessibility trait. It holds the key, which is shared by
 * all properties derived from the ProjectAccessibility property, as well as it defines defines
 * the (sound) fall back if the property is not computed but requested by another analysis.
 */
object FactoryMethod extends PropertyMetaInformation {

    /**
     * The key associated with every FactoryMethod property.
     * It contains the unique name of the property and the default property that
     * will be used if no analysis is able to (directly) compute the respective property.
     * `IsFactoryMethod` is chosen as default because we have to define a sound default value for
     * all depended analyses.
     */
    final val key: org.opalj.fpcf.PropertyKey = PropertyKey.create("FactoryMethod", IsFactoryMethod)
}

/**
 * The respective method is a factory method.
 */
case object IsFactoryMethod extends FactoryMethod { final val isRefineable: Boolean = false }

/**
 * The respective method is not a factory method.
 */
case object NotFactoryMethod extends FactoryMethod { final val isRefineable: Boolean = false }