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
package av
package checking

import scala.util.matching.Regex
import org.opalj.br.ClassFile
import org.opalj.br.VirtualSourceElement
import org.opalj.br.VirtualSourceElement.asVirtualSourceElements
import org.opalj.br.analyses.SomeProject
import org.opalj.bi.AccessFlagsMatcher

/**
 * A class matcher matches classes defined by the respective classes.
 *
 * @author Marco Torsello
 */
trait ClassMatcher extends ClassLevelMatcher {

    def matchMethods: Boolean

    def matchFields: Boolean

}

/**
 * Matches all project and library classes including inner elements like methods and fields defined by
 * the respective classes.
 *
 * @author Marco Torsello
 */
case object AllClasses extends ClassMatcher {

    def matchMethods: Boolean = true

    def matchFields: Boolean = true

    def doesMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = true

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        asVirtualSourceElements(project.allClassFiles)
    }

}

/**
 * Default class matcher matches classes defined by the respective classes.
 *
 * @author Marco Torsello
 */
case class DefaultClassMatcher(
        accessFlagsMatcher:       AccessFlagsMatcher   = AccessFlagsMatcher.ANY,
        namePredicate:            NamePredicate        = RegexNamePredicate(""".*""".r),
        annotationsPredicate:     AnnotationsPredicate = AnyAnnotations,
        matchSubclasses:          Boolean              = false,
        matchImplementingclasses: Boolean              = false,
        matchMethods:             Boolean              = true,
        matchFields:              Boolean              = true
) extends ClassMatcher {

    def isSubClass(classFile: ClassFile, project: SomeProject): Boolean = {
        var sourceClassFile: ClassFile = classFile
        while (sourceClassFile.superclassType.nonEmpty) {
            if (namePredicate(sourceClassFile.superclassType.get.fqn))
                return true;

            project.classFile(sourceClassFile.superclassType.get) match {
                case Some(cf) ⇒ sourceClassFile = cf
                case None ⇒
                    return false;
            }
        }

        false
    }

    def implementsInterface(classFile: ClassFile, project: SomeProject): Boolean = {
        classFile.interfaceTypes.exists(i ⇒ namePredicate(i.fqn))
    }

    def doesAnnotationMatch(classFile: ClassFile): Boolean = {
        annotationsPredicate(classFile.annotations)
    }

    def doesMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        val classFileName = classFile.thisType.fqn
        (namePredicate(classFileName) ||
            (matchSubclasses && isSubClass(classFile, project)) ||
            (matchImplementingclasses && implementsInterface(classFile, project))) &&
            (doesAnnotationMatch(classFile)) &&
            accessFlagsMatcher.unapply(classFile.accessFlags)
    }

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        asVirtualSourceElements(
            project.allClassFiles filter { doesMatch },
            matchMethods,
            matchFields
        )
    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[ClassMatcher]]s.
 *
 * @author Marco Torsello
 */
object ClassMatcher {

    def apply(namePredicate: NamePredicate): ClassMatcher = {
        new DefaultClassMatcher(namePredicate = namePredicate)
    }

    def apply(annotationsPredicate: AnnotationsPredicate): ClassMatcher = {
        new DefaultClassMatcher(annotationsPredicate = annotationsPredicate)
    }

    def apply(className: String): ClassMatcher = {
        require(className.indexOf('*') == -1)
        new DefaultClassMatcher(namePredicate = Equals(className))
    }

    def apply(className: String, matchPrefix: Boolean): ClassMatcher = {
        require(className.indexOf('*') == -1)
        val namePredicate =
            if (matchPrefix)
                StartsWith(className)
            else
                Equals(className)
        new DefaultClassMatcher(namePredicate = namePredicate)
    }

    def apply(
        className:    String,
        matchPrefix:  Boolean,
        matchMethods: Boolean,
        matchFields:  Boolean
    ): ClassMatcher = {
        require(className.indexOf('*') == -1)

        val namePredicate =
            if (matchPrefix)
                StartsWith(className)
            else
                Equals(className)

        new DefaultClassMatcher(
            namePredicate = namePredicate,
            matchMethods = matchMethods,
            matchFields = matchFields
        )
    }

    def apply(className: String, matchPrefix: Boolean, matchSubclasses: Boolean): ClassMatcher = {
        require(className.indexOf('*') == -1)

        val namePredicate =
            if (matchPrefix)
                StartsWith(className)
            else
                Equals(className)
        new DefaultClassMatcher(
            namePredicate = namePredicate,
            matchSubclasses = matchSubclasses
        )
    }

    def apply(regex: Regex): ClassMatcher = {
        new DefaultClassMatcher(namePredicate = RegexNamePredicate(regex))
    }

    def apply(regex: Regex, matchSubclasses: Boolean): ClassMatcher = {
        new DefaultClassMatcher(
            namePredicate = RegexNamePredicate(regex),
            matchSubclasses = matchSubclasses
        )
    }

}
