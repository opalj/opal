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

import org.opalj.br.ObjectType
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.ElementValuePairs
import org.opalj.br.analyses.SomeProject

/**
 * @inheritedDoc
 *
 * Defines commonly useful helper methods.
 *
 * @author Michael Eichberg
 */
abstract class AbstractPropertyMatcher extends org.opalj.fpcf.properties.PropertyMatcher {

    /**
     * Gets the value of the named element (`elementName`) of an annotation's element value pairs.
     * If the element values pairs do not contain the named element. The default values will
     * be looked up. This requires that the class file defining the annotation is part of the
     * analyzed code base.
     *
     * @param p The current project.
     * @param annotationType The type of the annotation.
     * @param evps The element value pairs of a concrete annotation of the given type.
     *          (Recall that elements with default values need not be specified and are also
     *          not stored; they need to be looked up in the defining class file if required.)
     * @param elementName The name of the element-value-pair.
     */
    def getValue(
        p:              SomeProject,
        annotationType: ObjectType,
        evps:           ElementValuePairs,
        elementName:    String
    ): ElementValue = {
        evps.collectFirst {
            case ElementValuePair(`elementName`, value) ⇒ value
        }.orElse {
            // get default value ...
            p.classFile(annotationType).get.findMethod(elementName).head.annotationDefault
        }.get
    }

}
