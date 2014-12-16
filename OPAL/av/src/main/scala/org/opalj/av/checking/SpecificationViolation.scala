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

import scala.language.implicitConversions

import java.net.URL

import scala.util.matching.Regex
import scala.collection.{ Map ⇒ AMap, Set ⇒ ASet }
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{ Map ⇒ MutableMap, HashSet }

import br._
import br.reader.Java8Framework.ClassFiles
import br.analyses.{ ClassHierarchy, Project, SomeProject }

import de._

/**
 * Used to report deviations between the specified and the implemented architecture.
 */
case class SpecificationViolation(
        project: SomeProject,
        dependencyChecker: DependencyChecker,
        source: VirtualSourceElement,
        sourceLineNumber: Int = 1,
        target: VirtualSourceElement,
        targetLineNumber: Int = 1,
        dependencyType: DependencyType,
        description: String) {

    override def toString(): String = {
        var javaSource = source.toJava
        var javaTarget = target.toJava

        if (javaSource.contains("{")) {
            javaSource = javaSource.substring(0, javaSource.indexOf("{"))
        }
        javaSource = "("+javaSource+".java:"+source.getLineNumber(project).getOrElse("1):"+source.toJava)+")"

        if (javaTarget.contains("{")) {
            javaTarget = javaTarget.substring(0, javaTarget.indexOf("{"))
        }
        javaTarget = "("+javaTarget+".java:"+target.getLineNumber(project).getOrElse("1):"+target.toJava)+")"

        description+" between "+dependencyChecker.sourceEnsembles.mkString(", ")+
            " and "+dependencyChecker.targetEnsembles.mkString(", ")+": "+
            javaSource+" "+dependencyType+" "+
            javaTarget
    }
}
