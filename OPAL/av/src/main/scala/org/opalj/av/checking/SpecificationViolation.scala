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

import scala.language.implicitConversions
import scala.language.existentials

import java.net.URL

import scala.Console.{RED, BLUE, RESET, BOLD}
import scala.util.matching.Regex
import scala.collection.{Map ⇒ AMap, Set ⇒ ASet}
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{Map ⇒ MutableMap, HashSet}

import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.analyses.{Project, SomeProject}

import org.opalj.de._

/**
 * Used to report deviations between the specified and the implemented architecture.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
sealed trait SpecificationViolation {

    final override def toString(): String = toString(useAnsiColors = false)

    def toString(useAnsiColors: Boolean): String

}

/**
 * Used to report deviations between the specified/expected and the implemented dependencies.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
case class DependencyViolation(
        project:           SomeProject,
        dependencyChecker: DependencyChecker,
        source:            VirtualSourceElement,
        target:            VirtualSourceElement,
        dependencyType:    DependencyType,
        description:       String
) extends SpecificationViolation {

    override def toString(useAnsiColors: Boolean): String = {

        val sourceLineNumber = source.getLineNumber(project).getOrElse(1)
        val javaSource = s"(${source.classType.toJava}.java:${sourceLineNumber})"
        val targetLineNumber = target.getLineNumber(project).getOrElse(1)
        val javaTarget = s"(${target.classType.toJava}.java:${targetLineNumber})"

        if (useAnsiColors)
            RED + description+
                " between "+BLUE + dependencyChecker.sourceEnsembles.mkString(", ") + RED+
                " and "+BLUE + dependencyChecker.targetEnsembles.mkString(", ") + RESET+": "+
                javaSource+" "+BOLD + dependencyType + RESET+" "+javaTarget
        else
            description+
                " between "+dependencyChecker.sourceEnsembles.mkString(", ")+
                " and "+dependencyChecker.targetEnsembles.mkString(", ")+": "+
                javaSource+" "+dependencyType+" "+javaTarget

    }

}

/**
 * Used to report source elements that have properties that deviate from the expected ones.
 *
 * @author Marco Torsello
 */
case class PropertyViolation(
        project:         SomeProject,
        propertyChecker: PropertyChecker,
        source:          VirtualSourceElement,
        propertyType:    String,
        description:     String
) extends SpecificationViolation {

    override def toString(useAnsiColors: Boolean): String = {

        val sourceLineNumber = source.getLineNumber(project).getOrElse(1)
        val javaSourceClass = s"(${source.classType.toJava}.java:$sourceLineNumber)"
        val javaSource =
            source match {
                case VirtualField(_, name, fieldType) ⇒
                    javaSourceClass + s" {${fieldType.toJava} $name}"
                case VirtualMethod(_, name, descriptor) ⇒
                    if (sourceLineNumber == 1)
                        javaSourceClass + s" {${descriptor.toJava(name)}}"
                    else
                        javaSourceClass
                case _ ⇒
                    javaSourceClass
            }

        if (useAnsiColors)
            RED + description+
                " between "+BLUE + propertyChecker.ensembles.mkString(", ") + RED+
                " and "+BLUE + propertyChecker.property + RESET+": "+
                javaSource+" "+BOLD + propertyType + RESET+" "+propertyChecker.property
        else
            description+
                " between "+propertyChecker.ensembles.mkString(", ")+
                " and "+propertyChecker.property+": "+
                javaSource+" "+propertyType+" "+propertyChecker.property

    }

}

