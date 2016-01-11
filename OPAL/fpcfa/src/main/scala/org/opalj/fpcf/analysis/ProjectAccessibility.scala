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

/**
 * This is a common trait for all ProjectAccessibility properties which can be emitted to the
 * PropertyStore. It describes the accessibility of a given entity.
 */
sealed trait ProjectAccessibility extends Property {
    final def key: org.opalj.fpcf.PropertyKey = ProjectAccessibility.key
}

/**
 * A companion object for the ProjectAccessibility trait. It holds the key, which is shared by
 * all properties derived from the ProjectAccessibility property, as well as it defines defines
 * the (sound) fall back if the property is not computed but requested by another analysis.
 */
object ProjectAccessibility extends PropertyMetaInformation {

    final val key: org.opalj.fpcf.PropertyKey = PropertyKey.create("Accessible", Global)

}

/**
 * Entities with `Global` project accessibility can be accessed by every entity within the project.
 */
case object Global extends ProjectAccessibility { final val isRefineable: Boolean = false }

/**
 * Entities with `PackageLocal` project accessibility can be accessed by every entity within
 * the package where the entity is defined.
 */
case object PackageLocal extends ProjectAccessibility { final val isRefineable: Boolean = false }

/**
 * Entities with `PackageLocal` project accessibility can be accessed by every entity within
 * the class where the entity is defined.
 */
case object ClassLocal extends ProjectAccessibility { final val isRefineable: Boolean = false }