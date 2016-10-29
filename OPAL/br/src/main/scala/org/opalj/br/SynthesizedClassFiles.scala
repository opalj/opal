/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

/**
 * This attribute stores references to [[ClassFile]] objects that have been generated
 * while parsing the annotated ClassFile.
 *
 * For example, to represent proxy types that have been created
 * by Java8 lambda or method reference expressions.
 *
 * This attribute may only be present while the class file is processed/read
 * and will be removed from the attributes table before any analysis sees the
 * "final" class file.
 *
 * This attribute may occur multiple times in the attributes table of a class file structure.
 *
 * @param 	classFiles A sequence consisting of class file objects and "reasons" why the
 * 			respective class file was created.
 *
 * @author 	Arne Lottmann
 * @author 	Michael Eichberg
 */
case class SynthesizedClassFiles(classFiles: List[(ClassFile, Option[AnyRef])]) extends Attribute {

    final override val kindId = SynthesizedClassFiles.KindId

    override def toString: String = {
        classFiles.map { cfAndReason ⇒
            val (cf, reason) = cfAndReason
            cf.thisType.toJava + (reason.map(r ⇒ s"/*$r*/").getOrElse(""))
        }.mkString("SynthesizedClassFiles(", ", ", ")")
    }
}

object SynthesizedClassFiles {
    final val KindId = 1002
}
