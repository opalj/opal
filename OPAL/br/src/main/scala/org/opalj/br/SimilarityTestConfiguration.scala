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
package br

/**
 * Specifies which parts of a class file should be compared with another one.
 *
 * @author Timothy Earley
 */
abstract class SimilarityTestConfiguration {

    /**
     * Selects those fields which should be compared. By default all fields are selected.
     */
    def compareFields(
        leftContext: ClassFile,
        left:        Seq[JVMField],
        right:       Seq[JVMField]
    ): (Seq[JVMField], Seq[JVMField])

    /**
     * Selects those methods which should be compared. By default all methods are selected.
     *
     * If, e.g., the `left` methods belong to the class which is derived from the `right` one
     * and should contain all methods except of the default constructor, then the default
     * constructor should be filtered from the right set of methods.
     */
    def compareMethods(
        leftContext: ClassFile,
        left:        Seq[JVMMethod],
        right:       Seq[JVMMethod]
    ): (Seq[JVMMethod], Seq[JVMMethod])

    /**
     * Selects the attributes which should be compared.
     */
    def compareAttributes(
        leftContext: CommonAttributes,
        left:        Attributes,
        right:       Attributes
    ): (Attributes, Attributes)

    def compareCode(
        leftContext: JVMMethod,
        left:        Option[Code],
        right:       Option[Code]
    ): (Option[Code], Option[Code])

}

class CompareAllConfiguration extends SimilarityTestConfiguration {

    override def compareFields(
        leftContext: ClassFile,
        left:        Seq[JVMField],
        right:       Seq[JVMField]
    ): (Seq[JVMField], Seq[JVMField]) = {
        (left, right)
    }

    override def compareMethods(
        leftContext: ClassFile,
        left:        Seq[JVMMethod],
        right:       Seq[JVMMethod]
    ): (Seq[JVMMethod], Seq[JVMMethod]) = {
        (left, right)
    }

    /**
     * Selects the attributes which should be compared. By default all attributes except
     * of unknown ones are selected.
     */
    override def compareAttributes(
        leftContext: CommonAttributes,
        left:        Attributes,
        right:       Attributes
    ): (Attributes, Attributes) = {
        val newLeft = left.filterNot(a ⇒ a.isInstanceOf[UnknownAttribute])
        val newRight = right.filterNot(a ⇒ a.isInstanceOf[UnknownAttribute])
        (newLeft, newRight)
    }

    override def compareCode(
        leftContext: JVMMethod,
        left:        Option[Code],
        right:       Option[Code]
    ): (Option[Code], Option[Code]) = {
        (left, right)
    }
}
object CompareAllConfiguration extends CompareAllConfiguration
