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
package da

import scala.xml.Node

import org.opalj.bi.AccessFlagsContexts.METHOD

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class Method_Info(
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes          = IndexedSeq.empty
) extends ClassMember {

    /**
     * The number of bytes required to store this method info object.
     */
    def size: Int = 2 + 2 + 2 + 2 /*attributes_count*/ + attributes.view.map(_.size).sum

    /**
     * The simple name of the method.
     */
    def name(implicit cp: Constant_Pool): String = cp(name_index).toString(cp)

    /**
     * The method descriptor as used by the Java VM. E.g., the method `void doIt()`
     * would have the descriptor `()V`.
     */
    def descriptor(implicit cp: Constant_Pool): String = cp(descriptor_index).asString

    /**
     * @param methodIndex The index of the methodn in the methods table. Required to create unique
     *                    ids/anchors.
     */
    def toXHTML(methodIndex: Int)(implicit cp: Constant_Pool): Node = {
        val (accessFlags, explicitAccessFlags) = accessFlagsToXHTML(access_flags, METHOD)

        val name = this.name
        val jvmDescriptor = descriptor
        val javaDescriptor = methodDescriptorAsJavaSignature(name, jvmDescriptor)
        val index = methodIndex.toString
        <div class="method" id={ name + jvmDescriptor } data-name={ name } data-index={ index } data-access-flags={ explicitAccessFlags }>
            <div class="method_signature">
                { accessFlags }
                <span>{ javaDescriptor }</span>
            </div>
            { attributesToXHTML(methodIndex) }
        </div>
    }

    private[this] def attributesToXHTML(methodIndex: Int)(implicit cp: Constant_Pool): Seq[Node] = {
        attributes map {
            case codeAttribute: Code_attribute ⇒ codeAttribute.toXHTML(methodIndex)
            case attribute                     ⇒ attribute.toXHTML
        }
    }
}
