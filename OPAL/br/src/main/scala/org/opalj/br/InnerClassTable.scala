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
package br

import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts

/**
 * Attribute in a class' attribute table the encodes information about inner classes.
 *
 * @author Michael Eichberg
 */
case class InnerClassTable(
    innerClasses: InnerClasses)
        extends Attribute {

    override def kindId: Int = InnerClassTable.KindId

}
object InnerClassTable {

    final val KindId = 9

}

case class InnerClass(
        innerClassType: ObjectType,
        outerClassType: Option[ObjectType],
        innerName: Option[String],
        innerClassAccessFlags: Int) {

    override def toString(): String = {
        "InnerClass"+
            "(type="+innerClassType.toJava+
            ",outerType="+outerClassType.map(_.toJava)+
            ",innerName="+innerName+
            ",accessFlags=\""+AccessFlags.toString(innerClassAccessFlags, AccessFlagsContexts.INNER_CLASS)+"\""+
            ")"
    }
}