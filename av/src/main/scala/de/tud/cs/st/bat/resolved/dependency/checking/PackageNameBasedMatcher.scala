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
package de.tud.cs.st
package bat
package resolved
package dependency
package checking

import analyses._

import scala.collection.{ Set }

/**
 * Matches all classes, fields and methods that are declared in the specified package.
 *
 * @param packageName The name of a package in binary notation.
 *      (I.e., "/" are used to separate a package name's segments; e.g.,
 *      "java/lang/Object").
 * @param matchSubpackages If true, all packages that start with the given package
 *      name are matched otherwise only classes declared in the given package are matched.
 *
 * @author Michael Eichberg
 */
case class PackageNameBasedMatcher(
    packageName: String,
    matchSubpackages: Boolean = false)
        extends SourceElementsMatcher {

    require(packageName.length >= 1)
    require(packageName.indexOf('*') == -1)
    require(packageName.indexOf('.') == -1)

    def extension(project: SomeProject): Set[VirtualSourceElement] = {
        val matchedClassFiles =
            project.classFiles.view filter { classFile ⇒
                val thisClassPackageName = classFile.thisType.packageName
                thisClassPackageName.startsWith(packageName) && (
                    matchSubpackages || thisClassPackageName.length() == packageName.length()
                )
            }
        matchCompleteClasses(matchedClassFiles)
    }

    override def toString = {
        var s = "\""+packageName.replace('/', '.')+".*"
        if (matchSubpackages)
            s += "*"
        s += "\""
        s
    }
}


