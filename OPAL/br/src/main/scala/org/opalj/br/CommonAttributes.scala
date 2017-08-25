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
 * Defines methods to return common attributes from the attributes table of
 * [[ClassFile]], [[Field]], [[Method]] and [[Code]] declarations.
 *
 * @author Michael Eichberg
 */
trait CommonAttributes {

    def attributes: Attributes

    def runtimeVisibleTypeAnnotations: TypeAnnotations = {
        attributes collectFirst { case RuntimeVisibleTypeAnnotationTable(vas) ⇒ vas } match {
            case Some(typeAnnotations) ⇒ typeAnnotations
            case None                  ⇒ IndexedSeq.empty
        }
    }

    def runtimeInvisibleTypeAnnotations: TypeAnnotations = {
        attributes collectFirst { case RuntimeInvisibleTypeAnnotationTable(ias) ⇒ ias } match {
            case Some(typeAnnotations) ⇒ typeAnnotations
            case None                  ⇒ IndexedSeq.empty
        }
    }

    final def foreachTypeAnnotation[U](f: TypeAnnotation ⇒ U): Unit = {
        runtimeVisibleTypeAnnotations.foreach(f)
        runtimeInvisibleTypeAnnotations.foreach(f)
    }

    /**
     * Compares this element's attributes with the given one.
     *
     * @return None, if both attribute lists are similar; Some(<description of the difference>)
     *         otherwise.
     */
    protected[this] def compareAttributes(
        other:  Attributes,
        config: SimilarityTestConfiguration
    ): Option[AnyRef] = {
        val (thisAttributes, otherAttributes) = config.compareAttributes(this, this.attributes, other)
        if (thisAttributes.size != otherAttributes.size) {
            val message =
                "number of (filtered) attributes differ: "+
                    (thisAttributes.toSet.diff(otherAttributes.toSet).mkString("{", ",", "}"))
            return Some((message, thisAttributes.size, otherAttributes.size));
        }
        // Recall that some attributes may be defined multiple times and therefore an easy
        // approach to get a stable sorting is not available.
        // (We have not seen any case of multiple occurences of an attribute in practice so far.)
        thisAttributes.find { a ⇒ !otherAttributes.exists(o ⇒ a.similar(o, config)) } map { missingAttribute ⇒
            val message = "missing attribute: "+missingAttribute
            return Some((message, thisAttributes, otherAttributes));
        }

        None
    }
}
