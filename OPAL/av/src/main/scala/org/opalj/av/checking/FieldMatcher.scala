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
package av
package checking

import scala.collection.Set
import org.opalj.br.ClassFile
import org.opalj.br.FieldType
import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.br.VirtualSourceElement
import org.opalj.br.Annotation

/**
 * Matches fields based on their name, type, annotations and declaring class.
 *
 * @author Marco Torsello
 * @author Michael Eichberg
 */
case class FieldMatcher(
    declaringClass: ClassLevelMatcher,
    annotations: AnnotationsPredicate,
    theType: Option[FieldType],
    theName: Option[NamePredicate])
        extends SourceElementsMatcher {

    def doesClassFileMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        declaringClass.doesMatch(classFile)
    }

    def doesFieldMatch(field: Field): Boolean = {
        (theType.isEmpty || (theType.get eq field.fieldType)) && (
            (theName.isEmpty || theName.get(field.name))) &&
            annotations(field.annotations)
    }

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        val allMatchedFields = project.allClassFiles collect {
            case classFile if doesClassFileMatch(classFile) ⇒ {
                classFile.fields collect {
                    case field if doesFieldMatch(field) ⇒ field.asVirtualField(classFile)
                }
            }
        }
        allMatchedFields.flatten.toSet
    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[FieldMatcher]]s.
 *
 * @author Marco Torsello
 */
object FieldMatcher {

    def apply(
        declaringClass: ClassLevelMatcher = AllClasses,
        annotationsPredicate: AnnotationsPredicate = AnyAnnotations,
        theType: Option[String] = None,
        theName: Option[String] = None,
        matchPrefix: Boolean = false): FieldMatcher = {

        assert(theName.isDefined || !matchPrefix)

        val nameMatcher: Option[NamePredicate] =
            theName match {
                case Some(f) ⇒
                    if (matchPrefix)
                        Some(StartsWith(f))
                    else
                        Some(Equals(f))
                case _ ⇒
                    None
            }

        new FieldMatcher(
            declaringClass,
            annotationsPredicate,
            theType.map(fqn ⇒ FieldType(fqn.replace('.', '/'))),
            nameMatcher)
    }

}
