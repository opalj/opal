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
 * <pre>
 * InnerClasses_attribute {
 * u2 attribute_name_index;
 * u4 attribute_length;
 * u2 number_of_classes; // => Seq[InnerClasses_attribute.Class]
 * {	u2 inner_class_info_index;
 * 	u2 outer_class_info_index;
 * 	u2 inner_name_index;
 * 	u2 inner_class_access_flags;
 * 	} classes[number_of_classes];
 * }
 * </pre>
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class InnerClasses_attribute(
        attribute_name_index: Int,
        innerClasses: Seq[InnerClassesEntry]) extends Attribute {

    def attribute_length = 2 + (innerClasses.size * 8)

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        throw new UnsupportedOperationException(
            "use \"toXHTML(definingClassFQN: String)(implicit cp: Constant_Pool): Node\""
        )
    }

    def toXHTML(definingClassFQN: String)(implicit cp: Constant_Pool): Node = {
        <div id="#innerClasses">
            <details>
                <summary class="attribute_name">{ cp(attribute_name_index).toString }</summary>
                {
                    for (innerClass ← innerClasses)
                        yield innerClass.toXHTML(definingClassFQN)(cp)
                }
            </details>
        </div>
    }
}
object InnerClasses_attribute {
    val name = "InnerClasses"
}

