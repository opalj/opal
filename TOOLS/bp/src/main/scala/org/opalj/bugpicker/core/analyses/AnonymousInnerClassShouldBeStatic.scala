/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.instructions._
import org.opalj.issues.Issue
import org.opalj.issues.IssueCategory
import org.opalj.issues.IssueKind
import org.opalj.issues.ClassLocation
import org.opalj.issues.Relevance

/**
 * This analysis reports anonymous inner classes that do not use their reference to the
 * parent class and as such could be made `static` in order to save some memory and
 * to improve overall comprehension.
 *
 * Since anonymous inner classes cannot be declared `static`, they must be refactored to
 * named inner classes first.
 *
 * @author Ralf Mitschke
 * @author Daniel Klauer
 * @author Peter Spieler
 * @author Florian Brandherm
 * @author Michael Eichberg
 */
object AnonymousInnerClassShouldBeStatic {

    def description: String = "Identifies anonymous inner classes that should be made static."

    private val withinAnonymousClass = "[$][0-9].*[$]".r

    /**
     * A heuristic for determining whether an inner class is inside an anonymous inner
     * class based on the class name.
     *
     * @param classFile The inner class to check.
     * @return Whether the inner class is inside an anonymous inner class.
     */
    private def isWithinAnonymousInnerClass(classFile: ClassFile): Boolean = {
        withinAnonymousClass.findFirstIn(classFile.thisType.fqn).isDefined
    }

    /**
     * Finds the last occurrence of either '$' or '+' in a class name string.
     *
     * @param fqn The class name to check.
     * @return The index of the last occurring '$' or '+', whichever is closer to the end.
     */
    private def lastIndexOfInnerClassEncoding(fqn: String): Int = {
        math.max(fqn.lastIndexOf('$'), fqn.lastIndexOf('+'))
    }

    /**
     * A heuristic for determining anonymous inner classes by the encoding in the name.
     *
     * @param classFile The class to check.
     * @return Whether the class is an anonymous inner class.
     */
    private def isAnonymousInnerClass(classFile: ClassFile): Boolean = {
        val fqn = classFile.thisType.fqn

        val lastSpecialChar = lastIndexOfInnerClassEncoding(fqn)
        if (lastSpecialChar < 0) {
            return false
        }

        val digitChar = lastSpecialChar + 1;
        digitChar < fqn.length && Character.isDigit(fqn.charAt(digitChar))
    }

    /**
     * A heuristic for determining whether the field points to the enclosing instance
     * by checking if its name starts with "this".
     *
     * @param field The field to check.
     * @return Whether the field is the inner class' reference to the parent object.
     */
    private def isOuterThisField(field: Field): Boolean = {
        field.name.startsWith("this$") || field.name.startsWith("this+")
    }

    /**
     * Checks whether a class has any methods which read the given field.
     *
     * Note: This assumes the class also declares this field, and that it's enough to
     * check for accesses through the context of that class. In other words, accesses
     * through the context of subclasses would not be detected, but that's not required
     * when checking the outer class reference field of an anonymous inner class.
     */
    private def hasMethodsReadingField(classFile: ClassFile, field: Field): Boolean = {
        for (MethodWithBody(body) <- classFile.methods) {
            if (body.instructions.exists {
                case FieldReadAccess(classFile.thisType, field.name, field.fieldType) => true
                case _ => false
            }) {
                return true;
            }
        }
        false
    }

    /**
     * Checks whether a class has any constructors with multiple ALOAD_1 instructions.
     */
    private def hasConstructorsWithMultipleALOAD_1s(classFile: ClassFile): Boolean = {
        for (method <- classFile.constructors; body <- method.body) {
            var count = 0
            body.instructions.foreach {
                case ALOAD_1 =>
                    count += 1;
                    if (count > 1) {
                        return true;
                    }
                case _ =>
            }
        }
        false
    }

    private def isOuterClassReferenceUsed(classFile: ClassFile): Answer = {
        // Try to find the outer class reference field.
        val outerClassReference = classFile.fields.find(isOuterThisField(_))
        if (outerClassReference.isEmpty) {
            return Unknown
        }

        // Any constructors with more than one access to their outer class reference
        // parameter? It's always read at least once, to store the outer class reference
        // into the outer class reference field.
        if (hasConstructorsWithMultipleALOAD_1s(classFile)) {
            return Yes
        }

        // Any methods reading the outer class reference field?
        if (hasMethodsReadingField(classFile, outerClassReference.get)) {
            return Yes
        }

        No
    }

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def apply(project: SomeProject, classFile: ClassFile): Iterable[Issue] = {
        if (project.isLibraryType(classFile) || classFile.isSynthetic)
            return None;

        if (!(isAnonymousInnerClass(classFile) &&
            !isWithinAnonymousInnerClass(classFile) &&
            isOuterClassReferenceUsed(classFile).isNo))
            return None;

        var supertype = classFile.superclassType.get.toJava

        if (classFile.interfaceTypes.nonEmpty) {
            val superInterfacetypes = classFile.interfaceTypes.map(_.toJava).mkString(" with ")

            if (classFile.superclassType.get == ObjectType.Object)
                supertype = superInterfacetypes
            else
                supertype += " implements "+superInterfacetypes

        }

        Some(
            Issue(
                "AnonymousInnerClassShouldBeStatic",
                Relevance.Low,
                s"this inner class of type $supertype should be made static",
                Set(IssueCategory.Comprehensibility, IssueCategory.Performance),
                Set(IssueKind.MissingStaticModifier),
                List(new ClassLocation(None, project, classFile))
            )
        )
    }
}
