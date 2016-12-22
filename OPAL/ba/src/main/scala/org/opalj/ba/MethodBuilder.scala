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
package ba

/**
 * Builder for a [[org.opalj.br.Method]]. A [[CodeAttributeBuilder]] can be added with the
 * apply method.
 *
 * @author Malte Limmeroth
 */
case class MethodBuilder(
        private var accessFlags: Int,
        private var name:        String,
        private var descriptor:  br.MethodDescriptor,
        private var attributes:  br.Attributes                = IndexedSeq.empty,
        private var body:        Option[CodeAttributeBuilder] = None
) extends ClassFileMemberBuilder {
    override private[ba] def addAccessFlags(accessFlags: Int): this.type = {
        this.accessFlags = this.accessFlags | accessFlags

        this
    }

    /**
     * Defines the [[org.opalj.br.Code]] attribute with the given [[CodeAttributeBuilder]].
     *
     * @see [[CODE$]]
     * @see [[CodeAttributeBuilder]]
     */
    def apply(codeAttributeBuilder: CodeAttributeBuilder): this.type = {
        body = Some(codeAttributeBuilder)

        this
    }

    /**
     * Returns the build [[org.opalj.br.Method]] and its annotations.
     */
    def buildMethod: (br.Method, Map[br.PC, AnyRef]) = {
        var tempAttributes = attributes
        var annotations = Map.empty[br.PC, AnyRef]
        if (body.isDefined) {
            val (code, tempAnnotations) = body.get.buildCodeAndAnnotations(accessFlags, name, descriptor)
            tempAttributes = tempAttributes :+ code
            annotations = tempAnnotations
        }

        val method = br.Method(
            accessFlags = accessFlags,
            name = name,
            descriptor = descriptor,
            attributes = tempAttributes
        )

        (method, annotations)
    }
}