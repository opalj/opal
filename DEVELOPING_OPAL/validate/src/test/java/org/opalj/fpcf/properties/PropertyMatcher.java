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
package org.opalj.fpcf.properties;

import scala.Option;
import scala.collection.immutable.List;
import scala.collection.immutable.Set;

import org.opalj.fpcf.Property;
import org.opalj.br.Annotation;
import org.opalj.br.ObjectType;
import org.opalj.br.analyses.Project;

/**
 * Defines a class that, when given a specific entity and its properties, validates if the
 * property is as expected. The class implicitly defines the expectation.
 * <p>
 * Concrete implementations should inherit from <code>AbstractPropertyMatcher</code>.
 * </p>
 *
 * @author Michael Eichberg
 */
public interface PropertyMatcher {

    /**
     * Called by the framework to test if executing the matcher - given the set of
     * actual analyses that are executed - is meaningful.
     *
     * @param p The project.
     * @param as The OPAL `ObjectType`'s of the executed analyses.
     */
    default boolean isRelevant(Project<?> p,Set<ObjectType> as, Annotation a) {
        return true;
    }

    /**
     * Tests if the computed property is matched by this matcher.
     *
     * @param p The project.
     * @param as The OPAL `ObjectType`'s of the executed analyses.
     * @param entity The annotated entity.
     * @param a The annotation.
     * @param properties '''All''' properties associated with the given entity.
     *
     * @return 'None' if the property was successfully matched; 'Some(<String>)' if the
     *          property was not successfully matched; the String describes the reason
     *          why the analysis failed.
     */
    Option<String> validateProperty(
            Project<?> p, Set<ObjectType> as,
            Object entity, Annotation a, List<Property> properties);

}
