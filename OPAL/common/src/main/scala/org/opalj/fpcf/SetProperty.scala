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

import org.opalj.collection.mutable.ArrayMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A set property is a property that is shared by a set of entities. A set property is
 * generally not refineable and not revokable.
 *
 * A [[SetProperty]] is compared with other properties using reference comparison. Hence, in
 * general a new [[SetProperty]] is created by creating an object that inherits from
 * [[SetProperty]]. For example:
 * {{{
 * object IsReachable extends SetProperty[Method]
 * }}}
 *
 * @author Michael Eichberg
 */
trait SetProperty[E <: AnyRef] extends PropertyKind {

    /**
     * A unique id that is automatically assigned with each instance of a `SetProperty` kind.
     */
    // the id is used to efficiently get the respective (identity) set
    final val id = { SetProperty.nextId(this.getClass().getSimpleName) }

    final val index: Int = -id - 1

    private[fpcf] final val mutex = new Object
}

private[fpcf] object SetProperty {

    private[this] final val idGenerator = new AtomicInteger(-1)

    private[this] final val theSetPropertyNames = ArrayMap[String](5)

    def propertyName(index: Int): String = {
        theSetPropertyNames.synchronized { theSetPropertyNames(index) }
    }

    def nextId(name: String): Int = {
        val n = if (name.endsWith("$")) name.substring(0, name.length() - 1) else name
        val nextId = idGenerator.getAndDecrement
        val index = -nextId - 1
        theSetPropertyNames.synchronized { theSetPropertyNames(index) = n }
        nextId
    }

}
