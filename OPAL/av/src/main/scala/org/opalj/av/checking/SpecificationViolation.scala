/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.Console.{RED, BLUE, RESET, BOLD}
import org.opalj.br._
import org.opalj.de._
import org.opalj.br.analyses.SomeProject

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
                case VirtualField(_, name, fieldType) =>
                    javaSourceClass + s" {${fieldType.toJava} $name}"
                case VirtualMethod(_, name, descriptor) =>
                    if (sourceLineNumber == 1)
                        javaSourceClass + s" {${descriptor.toJava(name)}}"
                    else
                        javaSourceClass
                case _ =>
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
