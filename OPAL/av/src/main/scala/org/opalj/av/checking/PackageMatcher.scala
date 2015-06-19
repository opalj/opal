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

import org.opalj.br._
import org.opalj.br.analyses.SomeProject

/**
 * Matches all classes in the specified package.
 *
 *
 * @author Marco Torsello
 */
case class PackageMatcher(
        nameMatcher: NameMatcher,
        classMatcher: Option[ClassMatcher]) extends ClassLevelMatcher {

    def doesMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        val packageName = classFile.thisType.packageName
        nameMatcher.doesMatch(packageName) &&
            (!classMatcher.isDefined || classMatcher.get.doesMatch(classFile))
    }

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        classMatcher.map(cm ⇒
            matchClasses(project.allClassFiles filter { doesMatch(_) }, cm.matchMethods, cm.matchFields))
            .getOrElse(matchClasses(project.allClassFiles filter { doesMatch(_) }))
    }
}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[PackageMatcher]]s.
 *
 * @author Marco Torsello
 */
object PackageMatcher {

    def apply(
        nameMatcher: NameMatcher,
        classMatcher: ClassMatcher): PackageMatcher = {
        new PackageMatcher(nameMatcher, Some(classMatcher))
    }

    /**
     * @param packageName The name of a package in java notation.
     *      (I.e., "." are used to separate a package name's segments; e.g.,
     *      "java.lang.Object").
     * @param matchSubpackages If true, all packages that start with the given package
     *      name are matched otherwise only classes declared in the given package are matched.
     *
     */
    def apply(
        packageName: String,
        matchSubpackages: Boolean = false): PackageMatcher = {
        new PackageMatcher(SimpleNameMatcher(packageName.replace('.', '/'), matchSubpackages), None)
    }

    /**
     * @param packageName The name of a package in java notation.
     *      (I.e., "." are used to separate a package name's segments; e.g.,
     *      "java.lang.Object").
     * @param classMatcher The [[ClassMatcher]] that should be matched.
     *
     */
    def apply(
        packageName: String,
        classMatcher: ClassMatcher): PackageMatcher = {
        new PackageMatcher(SimpleNameMatcher(packageName.replace('.', '/'), false), Some(classMatcher))
    }

    /**
     * @param packageName The name of a package in java notation.
     *      (I.e., "." are used to separate a package name's segments; e.g.,
     *      "java.lang.Object").
     * @param classMatcher The [[ClassMatcher]] that should be matched.
     * @param matchSubpackages If true, all packages that start with the given package
     *      name are matched otherwise only classes declared in the given package are matched.
     *
     */
    def apply(
        packageName: String,
        classMatcher: ClassMatcher,
        matchSubpackages: Boolean): PackageMatcher = {
        new PackageMatcher(SimpleNameMatcher(packageName.replace('.', '/'), matchSubpackages), Some(classMatcher))
    }
}
