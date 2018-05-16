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
package fpcf
package properties
package field_mutability

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.BooleanValue
import org.opalj.br.analyses.SomeProject

/**
 * Matches a field's `FieldMutability` property. The match is successful if the field either
 * does not have a corresponding property (in which case the fallback property will be
 * `NonFinalField`) or if the property is an instance of `NonFinalField`.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
class NonFinalMatcher extends AbstractPropertyMatcher {

    override def isRelevant(
        p:  SomeProject,
        as: Set[ObjectType],
        e:  Entity,
        a:  AnnotationLike
    ): Boolean = {
        val annotationType = a.annotationType.asObjectType

        val prematurelyRead = getValue(p, annotationType, a.elementValuePairs, "prematurelyRead").asInstanceOf[BooleanValue].value

        if (prematurelyRead) {
            val propertyStore = p.get(PropertyStoreKey)
            propertyStore(e, FieldPrematurelyRead.key) match {
                case FinalEP(_, PrematurelyReadField) ⇒ true
                case _                                ⇒ false
            }
        } else {
            true
        }
    }

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        if (properties.forall(p ⇒ p.isInstanceOf[NonFinalField] || p.key != FieldMutability.key))
            None
        else {
            Some(a.elementValuePairs.head.value.toString)
        }
    }

}
