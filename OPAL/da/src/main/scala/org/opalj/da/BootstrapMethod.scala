/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package org.opalj
package da

import scala.xml.Node

/**
 * @author Michael Eichberg
 */

case class BootstrapMethods_attribute(
        attribute_name_index: Int,
        methods:              Seq[BootstrapMethod]
) extends Attribute {

    override def attribute_length = 6 + 2 /*the size*/ + methods.size * 6

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <details>
            <summary>BootstrapMethods</summary>
            { methodsToXHTML(cp) }
        </details>
    }

    def methodsToXHTML(implicit cp: Constant_Pool) = {
        for (method ← methods) yield method.toXHTML(cp)
    }
}

object BootstrapMethods_attribute {
    val name = "BootstrapMethods"
}

case class BootstrapMethod(method_ref: Int, arguments: Seq[BootstrapArgument]) {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <details class="nested_details">
            <summary>{ cp(method_ref).asInlineNode }</summary>
            { argumentsToXHTML(cp) }
        </details>
    }

    def argumentsToXHTML(implicit cp: Constant_Pool) = {
        for (argument ← arguments) yield argument.toXHTML(cp)
    }
}

case class BootstrapArgument(cp_ref: Int) {

    def toXHTML(implicit cp: Constant_Pool): Node = {
        <div>{ cp(cp_ref).asInlineNode }</div>
    }

}
