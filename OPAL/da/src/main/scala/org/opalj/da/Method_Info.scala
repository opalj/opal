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
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class Method_Info(
        access_flags: Int,
        name_index: Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes: Attributes) {

    /**
     * @param definingTypeFQN The FQN of the class defining this field.
     */
    def toXHTML(methodIndex: Int)(implicit cp: Constant_Pool): Node = {
        val flags = methodAccessFlagsToString(access_flags)
        val filter_flags =
            org.opalj.bi.VisibilityModifier.get(access_flags) match {
                case None ⇒
                    val ac = flags
                    if (ac.length() == 0)
                        "default"
                    else
                        ac+" default"
                case _ ⇒
                    flags
            }

        val name = cp(name_index).toString(cp)
        <div class="method" name={ name } flags={ filter_flags }>
            <div class="method_signature">
                <span class="access_flags">{ flags }</span>
                <span>{ parseMethodDescriptor(name, cp(descriptor_index).asString) }</span>
                <a href="#" class="tooltip">{ name_index } <span>{ cp(name_index) }</span></a>
            </div>
            { attributesToXHTML(methodIndex) }
        </div>
    }

    private[this] def attributesToXHTML(methodIndex: Int)(implicit cp: Constant_Pool) = {
        for (attribute ← attributes) yield {
            attribute match {
                case codeAttribute: Code_attribute ⇒
                    codeAttribute.toXHTML(methodIndex)
                case _ ⇒
                    attribute.toXHTML(cp)
            }
        }
    }
}