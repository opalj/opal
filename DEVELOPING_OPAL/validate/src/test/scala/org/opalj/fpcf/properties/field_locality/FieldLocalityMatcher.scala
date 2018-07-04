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
package field_locality

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project

/**
 * A property matcher that checks whether the annotated method has the specified field locality
 * property.
 *
 * @author Florian Kuebler
 */
class FieldLocalityMatcher(val property: FieldLocality) extends AbstractPropertyMatcher {
    /**
     * Tests if the computed property is matched by this matcher.
     *
     * @param p          The project.
     * @param as         The OPAL `ObjectType`'s of the executed analyses.
     * @param entity     The annotated entity.
     * @param a          The annotation.
     * @param properties '''All''' properties associated with the given entity.
     * @return 'None' if the property was successfully matched; 'Some(<String>)' if the
     *         property was not successfully matched; the String describes the reason
     *         why the analysis failed.
     */
    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        if (!properties.exists {
            case `property` ⇒ true
            case _          ⇒ false
        }) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class NoLocalFieldMatcher extends FieldLocalityMatcher(properties.NoLocalField)

class ExtensibleLocalFieldMatcher extends FieldLocalityMatcher(properties.ExtensibleLocalField)

class LocalFieldMatcher extends FieldLocalityMatcher(properties.LocalField)

class ExtensibleLocalFieldWithGetterMatcher
    extends FieldLocalityMatcher(properties.ExtensibleLocalFieldWithGetter)

class LocalFieldWithGetterMatcher extends FieldLocalityMatcher(properties.LocalFieldWithGetter)
