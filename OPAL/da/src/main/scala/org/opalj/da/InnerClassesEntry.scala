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
import bi.AccessFlags
import bi.AccessFlagsContexts

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class InnerClassesEntry(
        inner_class_info_index: Int,
        outer_class_info_index: Int,
        inner_name_index: Int,
        inner_class_access_flags: Int) {

    def toXHTML(definingClassFQN: String)(implicit cp: Constant_Pool): Node = {
        val accessFlags =
            AccessFlags.toString(inner_class_access_flags, AccessFlagsContexts.INNER_CLASS)

        val outerClassFQN = cp(outer_class_info_index).toString

        if (definingClassFQN == outerClassFQN && inner_name_index != 0) {
            <li>
                <span class="access_flags">{ accessFlags } </span>
                <span class="sn tooltip">
                    { cp(inner_name_index).toString }
                    <span>
                        Defined Type:
                        <span class="fqn">
                            { cp(inner_class_info_index).toString }
                        </span>
                    </span>
                </span>
            </li>
        } else {
            <li>
                <span class="fqn">
                    { outerClassFQN }
                    {{
                    <span class="access_flags">{ accessFlags } </span>
                    {
                        if (inner_name_index != 0)
                            <span class="sn">
                                { cp(inner_name_index).toString }
                                <span>
                                    Defined Type:
                                    <span class="fqn">
                                        { cp(inner_class_info_index).toString }
                                    </span>
                                </span>
                            </span>
                        else
                            <span class="fqn tooltip">
                                { cp(inner_class_info_index).toString }
                                <span>Anonymous Type</span>
                            </span>
                    }
                    }}
                </span>
            </li>

        }
    }
}


