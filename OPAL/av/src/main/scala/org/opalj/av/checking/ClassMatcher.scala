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
import scala.util.matching.Regex
import org.opalj.br.ClassFile
import org.opalj.br.VirtualSourceElement
import org.opalj.br.analyses.SomeProject
import org.opalj.bi.AccessFlagsMatcher

/**
 * A class matcher matches classes defined by the respective classes.
 *
 * @author Marco Torsello
 */
case class ClassMatcher(
        accessFlagsMatcher: AccessFlagsMatcher = AccessFlagsMatcher.ALL,
        nameMatcher: NameMatcher = RegexNameMatcher(""".*""".r),
        annotationMatcher: Option[AnnotationMatcher] = None,
        matchSubclasses: Boolean = false,
        matchImplementingclasses: Boolean = false,
        matchMethods: Boolean = true,
        matchFields: Boolean = true) extends ClassLevelMatcher {

    def isSubClass(classFile: ClassFile, project: SomeProject): Boolean = {
        var sourceClassFile: Option[ClassFile] = Some(classFile)
        while (!sourceClassFile.isEmpty && !sourceClassFile.get.superclassType.isEmpty) {
            if (nameMatcher.doesMatch(sourceClassFile.get.superclassType.get.fqn)) return true
            sourceClassFile = project.classFile(sourceClassFile.get.superclassType.get)
        }

        false
    }

    def implementsInterface(classFile: ClassFile, project: SomeProject): Boolean = {
        classFile.interfaceTypes.exists(i ⇒ nameMatcher.doesMatch(i.fqn))
    }

    def doesAnnotationMatch(classFile: ClassFile): Boolean = {
        classFile.annotations.exists(annotationMatcher.get.doesMatch(_))
    }

    def doesMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        val classFileName = classFile.thisType.fqn
        (nameMatcher.doesMatch(classFileName) ||
            (matchSubclasses && isSubClass(classFile, project)) ||
            (matchImplementingclasses && implementsInterface(classFile, project))) &&
            (annotationMatcher.isEmpty || doesAnnotationMatch(classFile)) &&
            accessFlagsMatcher.unapply(classFile.accessFlags)
    }

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        matchClasses(project.allClassFiles filter { doesMatch(_) }, matchMethods, matchFields)
    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[ClassMatcher]]s.
 *
 * @author Marco Torsello
 */
object ClassMatcher {

    def apply(nameMatcher: NameMatcher): ClassMatcher = {
        new ClassMatcher(nameMatcher = nameMatcher)
    }

    def apply(annotationMatcher: AnnotationMatcher): ClassMatcher = {
        new ClassMatcher(annotationMatcher = Some(annotationMatcher))
    }

    def apply(className: String): ClassMatcher = {
        require(className.indexOf('*') == -1)
        new ClassMatcher(nameMatcher = SimpleNameMatcher(className.replace('.', '/'), false))
    }

    def apply(className: String, matchPrefix: Boolean): ClassMatcher = {
        require(className.indexOf('*') == -1)
        new ClassMatcher(nameMatcher = SimpleNameMatcher(className.replace('.', '/'), matchPrefix))
    }

    def apply(
        className: String,
        matchPrefix: Boolean,
        matchMethods: Boolean,
        matchFields: Boolean): ClassMatcher = {
        require(className.indexOf('*') == -1)
        new ClassMatcher(
            nameMatcher = SimpleNameMatcher(className.replace('.', '/'), matchPrefix = matchPrefix),
            matchMethods = matchMethods,
            matchFields = matchFields
        )
    }

    def apply(className: String, matchPrefix: Boolean, matchSubclasses: Boolean): ClassMatcher = {
        require(className.indexOf('*') == -1)
        new ClassMatcher(nameMatcher = SimpleNameMatcher(className.replace('.', '/'), matchPrefix), matchSubclasses = matchSubclasses)
    }

    def apply(matcher: Regex): ClassMatcher = {
        new ClassMatcher(nameMatcher = RegexNameMatcher(matcher))
    }

    def apply(matcher: Regex, matchSubclasses: Boolean): ClassMatcher = {
        new ClassMatcher(nameMatcher = RegexNameMatcher(matcher), matchSubclasses = matchSubclasses)
    }

}
