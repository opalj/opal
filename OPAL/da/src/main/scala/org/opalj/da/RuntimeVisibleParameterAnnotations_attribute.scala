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
 *
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class RuntimeVisibleParameterAnnotations_attribute(
        attribute_name_index: Int,
        attribute_length: Int,
        parameter_annotations: IndexedSeq[IndexedSeq[Annotation]]) extends Attribute {

    type ParameterAnnotations = IndexedSeq[IndexedSeq[Annotation]]

    def attribute_name = RuntimeVisibleParameterAnnotations_attribute.name

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        <div class="annotation">//RuntimeVisibleParameterAnnotations:{ annotationstoXHTML(cp) }</div>
    }

    def annotationstoXHTML(implicit cp: Constant_Pool): Node = {
        <span>
          { for (parameter_annotation ← parameter_annotations)for (annotation ← parameter_annotation) yield annotation.toXHTML(cp) }
      </span>
    }
}

object RuntimeVisibleParameterAnnotations_attribute {

    val name = "RuntimeVisibleParameterAnnotations"

}