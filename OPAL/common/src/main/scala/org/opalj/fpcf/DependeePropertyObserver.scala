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
package org.opalj.fpcf

import java.util.concurrent.atomic.AtomicBoolean

/**
 * A property observer that can be associated with multiple
 * entities and which ensures that the property update event is propagated at most once and
 * which also removes itself from all observed entities by calling the given `deregisterObserver`
 * method.
 *
 * @author Michael Eichberg
 */
private[fpcf] abstract class DependeePropertyObserver(
        final val dependerEPK:        SomeEPK,
        final val deregisterObserver: (SomeEPK) ⇒ _
) extends PropertyObserver {

    private[this] val isExecuted = new AtomicBoolean(false)

    final def deregister(): Unit = {
        val isAlreadyExecuted = isExecuted.getAndSet(true)
        if (!isAlreadyExecuted) {
            deregisterObserver(dependerEPK)
        }
    }

    final override def apply(e: Entity, p: Property, u: UpdateType): Unit = {
        val isNotYetExecuted = isExecuted.compareAndSet(false, true)
        if (isNotYetExecuted) {
            deregisterObserver(dependerEPK)
            propertyChanged(e, p, u)
        }
    }

    def propertyChanged(e: Entity, p: Property, u: UpdateType): Unit

    override def toString: String = {
        val id = System.identityHashCode(this).toHexString
        s"ShareablePropertyObserver(dependerEPK=$dependerEPK,isExecuted=${isExecuted.get},id=$id)"
    }
}
