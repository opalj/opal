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
import scala.xml.NodeSeq

import org.opalj.bi.AccessFlagsContexts.FIELD

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
case class Field_Info(
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes          = IndexedSeq.empty
) extends ClassMember {

    def size: Int = 2 + 2 + 2 + 2 /* attributes_count*/ + attributes.view.map(_.size).sum

    /**
     * The field's name.
     */
    def fieldName(implicit cp: Constant_Pool): String = cp(name_index).asString

    /**
     * The type of the field.
     */
    def fieldType(implicit cp: Constant_Pool): FieldTypeInfo = {
        parseFieldType(cp(descriptor_index).asString)
    }

    def toXHTML(implicit cp: Constant_Pool): Node = {
        val (accessFlags, explicitAccessFlags) = accessFlagsToXHTML(access_flags, FIELD)
        val (constantValue, otherAttributes) =
            attributes.partition(a ⇒ a.attribute_name == "ConstantValue")
        val fieldName = this.fieldName
        val fieldDeclaration =
            <span class="field_declaration">
                { accessFlags }
                { fieldType.asSpan("field_type") }
                <span class="name">{ fieldName }</span>
                { if (constantValue.nonEmpty) constantValue.head.toXHTML else NodeSeq.Empty }
            </span>

        if (otherAttributes.isEmpty) {
            <div class="field details" data-name={ fieldName } data-access-flags={ explicitAccessFlags }>
                { fieldDeclaration }
            </div>
        } else {
            <details class="field" data-name={ fieldName } data-access-flags={ explicitAccessFlags }>
                <summary>{ fieldDeclaration }</summary>
                { otherAttributes.map(_.toXHTML) }
            </details>
        }
    }

    // def attributesToXHTML(implicit cp: Constant_Pool): Seq[Node] = {
    //    attributes.map(_.toXHTML)
    // }

}
