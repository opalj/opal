/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package av
package checking

import org.opalj.br.MethodDescriptor
import org.opalj.br.Attributes
import org.opalj.br.Method
import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts
import org.opalj.br.Attribute
import org.opalj.bi.AccessFlagsMatcher

/**
 * @author Marco Torsello
 */
case class MethodAttributesMatcher(
        accessFlags: AccessFlagsMatcher = AccessFlagsMatcher.ALL,
        name: String,
        descriptor: Option[MethodDescriptor],
        attributes: Attributes) {

    def doesMatch(method: Method): Boolean = {
        accessFlags.unapply(method.accessFlags) &&
            ((descriptor.isEmpty && method.name == name) ||
                (descriptor.nonEmpty && method.hasSameSignature(name, descriptor.get))) &&
                (attributes.isEmpty ||
                    (method.attributes.size == attributes.size &&
                        attributes.forall(a ⇒
                            method.attributes.exists(_ == a))))
    }

    def toDescription(): String = {
        val descriptorString = descriptor match {
            case Some(d) ⇒ d.toJava(name)
            case _       ⇒ name
        }

        accessFlags.toString + descriptorString +
            attributes.view.map(_.getClass.getSimpleName).mkString(" « ", ", ", " »")
    }
}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[MethodAttributesMatcher]]s.
 *
 * @author Marco Torsello
 */
object MethodAttributesMatcher {

    def apply(
        accessFlags: AccessFlagsMatcher,
        name: String,
        descriptor: MethodDescriptor): MethodAttributesMatcher = {
        new MethodAttributesMatcher(accessFlags, name, Some(descriptor), Seq.empty[Attribute])
    }

    def apply(
        accessFlags: AccessFlagsMatcher,
        name: String): MethodAttributesMatcher = {
        new MethodAttributesMatcher(accessFlags, name, None, Seq.empty[Attribute])
    }

    def apply(
        name: String): MethodAttributesMatcher = {
        new MethodAttributesMatcher(name = name, descriptor = None, attributes = Seq.empty[Attribute])
    }

}